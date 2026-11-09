#version 120

/* DRAWBUFFERS:0 */

#include "/lib/loi_di_nhap_sky.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;
uniform vec4 entityColor;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

const float GIANT_SEA_LEVEL = 65.0;
const float GIANT_MIST_TOP = 95.0;      // sương lên tới độ cao này trên mặt biển: tăng thì nuốt khối nhiều hơn
const float GIANT_AIR = 0.28;           // phần trên vẫn hơi nhạt theo không khí: 0 là rõ hẳn
const float GIANT_SHADE_TOP = 0.92;     // độ sáng mặt trên, hai mặt bên trái phải, và mặt trước sau
const float GIANT_SHADE_X = 0.66;
const float GIANT_SHADE_Z = 0.50;
const float GIANT_TILE = 4.0;            // cạnh mỗi ô gạch (block): giảm thì nhiều ô hơn
const float GIANT_GROUT = 0.05;         // bề rộng mạch, tính theo phần của một ô
const vec3  GIANT_TILE_COLOR = vec3(0.93, 0.91, 0.87);
const vec3  GIANT_GROUT_COLOR = vec3(0.50, 0.47, 0.46);
const vec3  GIANT_MIST_COLOR = vec3(0.98, 0.70, 0.66);

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * color.rgb * light.rgb;
    vanillaColor = mix(vanillaColor, entityColor.rgb, entityColor.a);

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);
    vec3 relative = mat3(gbufferModelViewInverse) * viewPos;
    vec3 skyBehind = ldnGradient(normalize(relative));
    vec3 finalColor = mix(vanillaColor, fogColor, fogFactor);

    // Các khối khổng lồ (LoiDiNhapKhoi) mọc lên từ biển mây: sương theo độ cao, chân tan hẳn, đỉnh rõ.
    // Chỉ áp dụng ở xa hơn tầm chunk, nơi không có thực thể nào khác.
    float giant = smoothstep(110.0, 150.0, dist);
    if (giant > 0.0) {
        vec3 world = relative + cameraPosition;
        float height = world.y - GIANT_SEA_LEVEL;
        // Mép sương gợn lên xuống như mặt trên của mây
        float billow = ldnNoise(world.xz * 0.014 + vec2(world.y * 0.006)) * 0.65
                     + ldnNoise(world.xz * 0.045 + vec2(frameTimeCounter * 0.01)) * 0.35;
        float top = GIANT_MIST_TOP * (0.55 + 0.9 * billow);
        float mist = 1.0 - smoothstep(top * 0.20, top, height);

        // Mỗi mặt một độ sáng, không thì khối bị cháy thành mảng phẳng
        vec3 faceNormal = abs(normalize(cross(dFdx(relative), dFdy(relative))));
        float shade = faceNormal.y * GIANT_SHADE_TOP + faceNormal.x * GIANT_SHADE_X + faceNormal.z * GIANT_SHADE_Z;
        vec3 surface = vanillaColor;

        // Khối trắng / gạch men được lát hàng trăm ô nhỏ theo toạ độ thế giới, để thấy nó to tới đâu
        float greyness = max(albedo.r, max(albedo.g, albedo.b)) - min(albedo.r, min(albedo.g, albedo.b));
        if (greyness < 0.12 && dot(albedo.rgb, vec3(0.333)) > 0.55) {
            vec2 face = faceNormal.y > 0.5 ? world.xz : (faceNormal.x > 0.5 ? world.zy : world.xy);
            vec2 cell = face / GIANT_TILE;
            vec2 id = floor(cell);
            vec2 f = fract(cell);
            vec2 pixel = fwidth(cell);
            vec2 edge = min(f, 1.0 - f);
            vec2 lines = 1.0 - smoothstep(vec2(GIANT_GROUT), vec2(GIANT_GROUT) + pixel * 1.4, edge);
            // Xa quá thì mạch không còn chia được thành pixel: nhạt đi thay vì nhấp nháy
            float grout = max(lines.x, lines.y) * (1.0 - smoothstep(0.35, 0.9, max(pixel.x, pixel.y)));

            float tone = 0.86 + 0.14 * ldnHash(id);
            float stain = ldnNoise(face * 0.035) * 0.6 + ldnNoise(face * 0.11) * 0.4;
            vec3 tile = GIANT_TILE_COLOR * tone * (0.84 + 0.22 * stain);
            surface = mix(tile, GIANT_GROUT_COLOR, grout);
        }

        vec3 clear = mix(surface * shade, skyBehind, GIANT_AIR);
        vec3 giantColor = mix(clear, GIANT_MIST_COLOR, mist);
        finalColor = mix(finalColor, giantColor, giant);
    }

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
