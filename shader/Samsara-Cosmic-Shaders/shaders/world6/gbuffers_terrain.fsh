#version 120

/* DRAWBUFFERS:05 */

uniform sampler2D tex;
uniform sampler2D specular;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 worldPos;

#include "/lib/dem_dau_tien_tear.glsl"

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    float inTear = tearMask(worldPos);
    if (inTear < 0.5) {
        // Ngoài vết rách: bản Alpha. Khối không có bản Alpha (alpha = 0) là khối của bản mới: ẩn đi.
        vec4 old = texture2D(specular, texCoord);
        if (old.a < 0.004) discard;
        // alpha 254: texture đã tô sẵn màu (Golden Days), lưu một nửa giá trị để chia ngược màu biome không tràn
        if (old.a > 0.9 && old.a < 0.999) old.rgb *= 2.0;
        albedo = vec4(old.rgb, old.a > 0.1 ? 1.0 : old.a);
    }
    if (albedo.a < 0.1) discard;

    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * color.rgb * light.rgb;
    // Bên trong vết rách là một lúc khác: hơi tối và lạnh hơn bên ngoài (nhẹ thôi, để cỏ bản mới vẫn đúng màu)
    if (inTear > 0.5) vanillaColor *= vec3(0.86, 0.88, 0.94);

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    gl_FragData[0] = vec4(mix(vanillaColor, fogColor, fogFactor), 1.0);
    // colortex5.r = pixel thuộc vết rách: bước final tách màu kiểu glitch ở đó
    gl_FragData[1] = vec4(inTear, 0.0, 0.0, 1.0);
}
