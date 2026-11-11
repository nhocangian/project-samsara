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
const float GIANT_GROUT = 0.05;
const float GIANT_TILE_MAJOR = 4.0;     // cứ chừng này ô thì có một đường mạch lớn
const float GIANT_GROUT_MAJOR = 0.02;
const float GIANT_AIR_NEAR = 400.0;     // từ khoảng cách này khối bắt đầu nhạt thêm theo không khí
const float GIANT_AIR_FAR = 1800.0;
const float GIANT_AIR_EXTRA = 0.38;
const float GIANT_AIR_EDGE = 4500.0;    // các tháp ở tận chân trời nhạt thêm một bậc
const float GIANT_AIR_EDGE_EXTRA = 0.14;
const float GIANT_ANGEL_AIR = 0.45;     // Ophanim nhạt theo không khí bằng chừng này phần so với khối         // bề rộng mạch, tính theo phần của một ô
const vec3  GIANT_TILE_COLOR = vec3(0.93, 0.91, 0.87);
const vec3  GIANT_GROUT_COLOR = vec3(0.50, 0.47, 0.46);
const float GIANT_SUN_FOCUS = 9.0;      // tăng thì vầng chói gom sát quanh mặt trời hơn
const float GIANT_SUN_GLARE = 0.75;     // độ chói ở phần khối gần mặt trời
const float GIANT_SUN_FACE = 0.18;      // mặt quay về phía nắng sáng thêm
const vec3  GIANT_SUN_COLOR = vec3(1.00, 0.82, 0.62);
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
    // Ophanim (LoiDiNhapAngel, alpha đỉnh 253): sương, nắng như khối khổng lồ nhưng không lát gạch và nhạt ít hơn
    bool angel = color.a < 0.994;
    // Chim ở xa (LoiDiNhapBirds, alpha 254): chỉ nhạt đi theo không khí
    bool bird = color.a < 0.999 && !angel;
    if (bird) finalColor = mix(vanillaColor, skyBehind, 0.30 * giant);
    if (giant > 0.0 && !bird) {
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
        if (angel) shade = 0.72 + 0.16 * faceNormal.y;   // mắt và lông quay về camera, không được tối như mặt khối
        vec3 surface = vanillaColor;

        // Khối trắng / gạch men được lát hàng trăm ô nhỏ theo toạ độ thế giới, để thấy nó to tới đâu
        float greyness = max(albedo.r, max(albedo.g, albedo.b)) - min(albedo.r, min(albedo.g, albedo.b));
        if (!angel && greyness < 0.12 && dot(albedo.rgb, vec3(0.333)) > 0.55) {
            vec2 face = faceNormal.y > 0.5 ? world.xz : (faceNormal.x > 0.5 ? world.zy : world.xy);
            vec2 cell = face / GIANT_TILE;
            vec2 id = floor(cell);
            vec2 f = fract(cell);
            vec2 pixel = fwidth(cell);
            vec2 edge = min(f, 1.0 - f);
            vec2 lines = 1.0 - smoothstep(vec2(GIANT_GROUT), vec2(GIANT_GROUT) + pixel * 1.4, edge);
            // Xa quá thì mạch không còn chia được thành pixel: nhạt đi thay vì nhấp nháy
            float grout = max(lines.x, lines.y) * (1.0 - smoothstep(0.35, 0.9, max(pixel.x, pixel.y)));
            // Đường lớn mỗi GIANT_TILE_MAJOR ô: ở rất xa vẫn còn thấy, mặt khối không thành mảng phẳng
            vec2 major = face / (GIANT_TILE * GIANT_TILE_MAJOR);
            vec2 majorPixel = fwidth(major);
            vec2 majorEdge = min(fract(major), 1.0 - fract(major));
            vec2 majorLines = 1.0 - smoothstep(vec2(GIANT_GROUT_MAJOR), vec2(GIANT_GROUT_MAJOR) + majorPixel * 1.4, majorEdge);
            grout = max(grout, max(majorLines.x, majorLines.y) * (1.0 - smoothstep(0.35, 0.9, max(majorPixel.x, majorPixel.y))));

            float tone = 0.86 + 0.14 * ldnHash(id);
            float stain = ldnNoise(face * 0.035) * 0.6 + ldnNoise(face * 0.11) * 0.4;
            vec3 tile = GIANT_TILE_COLOR * tone * (0.84 + 0.22 * stain);
            surface = mix(tile, GIANT_GROUT_COLOR, grout);
        }

        // Càng xa càng chìm vào không khí: tạo các lớp chiều sâu
        float air = GIANT_AIR + smoothstep(GIANT_AIR_NEAR, GIANT_AIR_FAR, dist) * GIANT_AIR_EXTRA
                  + smoothstep(GIANT_AIR_FAR, GIANT_AIR_EDGE, dist) * GIANT_AIR_EDGE_EXTRA;
        if (angel) air *= GIANT_ANGEL_AIR;
        vec3 clear = mix(surface * shade, skyBehind, air);
        // Phần khối nằm gần mặt trời trên màn hình bị nắng chói lấn vào, mặt quay về phía nắng sáng hơn
        vec3 sunDir = normalize(LDN_SUN_POS);
        float nearSun = pow(max(dot(normalize(relative), sunDir), 0.0), GIANT_SUN_FOCUS);
        vec3 signedNormal = normalize(cross(dFdx(relative), dFdy(relative)));
        if (dot(signedNormal, relative) > 0.0) signedNormal = -signedNormal;
        float facing = max(dot(signedNormal, sunDir), 0.0);
        // Ophanim chỉ nhận vầng chói ở rìa gần mặt trời, không cộng nắng lên mặt (cộng vào là cháy trắng)
        clear += GIANT_SUN_COLOR * (angel ? nearSun * GIANT_SUN_GLARE * 0.5 : nearSun * GIANT_SUN_GLARE + facing * GIANT_SUN_FACE);
        vec3 giantColor = mix(clear, GIANT_MIST_COLOR, mist);
        finalColor = mix(finalColor, giantColor, giant);
    }

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
