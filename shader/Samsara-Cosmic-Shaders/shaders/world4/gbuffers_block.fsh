#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;

const vec3 KHOI_NHAP_COLOR = vec3(1.0, 1.0, 0.98);

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * color.rgb * light.rgb * vec3(1.0, 0.90, 0.86);

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    vec3 finalColor = mix(vanillaColor, fogColor, fogFactor);

    // Thanh viền Khối Nháp: texture một màu (8, 8, 10), riêng tầng này vẽ thành trắng.
    // Nếu đổi màu trong mod/tools/generate_khoi_nhap.py thì phải đổi cả số ở đây.
    if (distance(albedo.rgb, vec3(8.0, 8.0, 10.0) / 255.0) < 0.008) finalColor = KHOI_NHAP_COLOR;

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
