#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;
varying vec3 viewPos;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    float blockLight = clamp((lmCoord.x - 0.03125) / 0.9375, 0.0, 1.0);
    float skyLight   = clamp((lmCoord.y - 0.03125) / 0.9375, 0.0, 1.0);

    vec3 wNorm = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
    vec3 worldNormal = length(wNorm) > 0.001 ? normalize(wNorm) : vec3(0.0, 1.0, 0.0);

    // 1. Ánh sáng vàng kim hổ phách chiếu rọi trực tiếp từ Hắc Lỗ Gargantua:
    vec3 bhDir = normalize(vec3(0.0, 0.54, -0.84));
    float NdotBH = dot(worldNormal, bhDir);
    float bhDiffuse = clamp(NdotBH * 0.65 + 0.35, 0.0, 1.0);
    vec3 bhLightCol = vec3(1.35, 0.95, 0.55);
    vec3 bhDirect = bhLightCol * bhDiffuse * clamp(skyLight * 1.1 + 0.30, 0.30, 1.30);

    // 2. Hào quang tán xạ vũ trụ từ vòm trời (Cosmic Ambient):
    float zenithFactor = clamp(worldNormal.y * 0.5 + 0.5, 0.25, 1.0);
    vec3 cosmicAmbient = vec3(0.25, 0.16, 0.10) * zenithFactor;

    // 3. Phản quang bề mặt tay/vật phẩm (Fresnel Rim Lighting):
    float fresnel = pow(clamp(1.0 - abs(normal.z), 0.0, 1.0), 2.5);
    vec3 rimGlow = vec3(0.95, 0.65, 0.35) * (fresnel * 0.35);

    // 4. Ngọn lửa linh hồn tím/lam từ đuốc và đèn
    float torchFactor = pow(blockLight, 2.0);
    vec3 soulTorch = mix(vec3(0.22, 0.88, 1.0), vec3(0.88, 0.35, 1.0), sin(frameTimeCounter * 2.0) * 0.25 + 0.5) * (torchFactor * 1.8);

    // 5. Ánh sáng hư vô thăm thẳm
    vec3 voidAmbient = vec3(0.06, 0.035, 0.05);

    // Tổng hợp ánh sáng
    vec3 totalLight = voidAmbient + cosmicAmbient + bhDirect + soulTorch + rimGlow;

    vec4 vanillaLight = texture2D(lightmap, lmCoord);
    totalLight *= clamp(vanillaLight.rgb * 1.2 + 0.20, 0.30, 1.40);

    vec3 litColor = albedo.rgb * color.rgb * totalLight;

    gl_FragData[0] = vec4(litColor, albedo.a);
}
