#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;
uniform float far;
uniform int isEyeInWater;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;
varying vec3 viewPos;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    float t = frameTimeCounter * 0.5;

    // Chuẩn hóa tọa độ ánh sáng [0.0, 1.0]
    float blockLight = clamp((lmCoord.x - 0.03125) / 0.9375, 0.0, 1.0);
    float skyLight   = clamp((lmCoord.y - 0.03125) / 0.9375, 0.0, 1.0);

    // Vector pháp tuyến thế giới
    vec3 wNorm = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
    vec3 worldNormal = length(wNorm) > 0.001 ? normalize(wNorm) : vec3(0.0, 1.0, 0.0);

    // 1. Ánh sáng vàng kim hổ phách chiếu rọi trực tiếp từ Hắc Lỗ Gargantua:
    vec3 bhDir = normalize(vec3(0.0, 0.54, -0.84));
    float NdotBH = dot(worldNormal, bhDir);
    float bhDiffuse = clamp(NdotBH * 0.65 + 0.35, 0.0, 1.0);
    vec3 bhLightCol = vec3(1.65, 1.25, 0.75);
    vec3 bhDirect = bhLightCol * bhDiffuse * clamp(skyLight * 1.15 + 0.44, 0.44, 1.55);

    // 2. Hào quang tán xạ vũ trụ từ vòm trời (Cosmic Ambient):
    float zenithFactor = clamp(worldNormal.y * 0.5 + 0.5, 0.25, 1.0);
    vec3 cosmicAmbient = vec3(0.38, 0.25, 0.18) * zenithFactor;

    // 3. Phản quang bề mặt khối (Fresnel & Specular Glint):
    vec3 viewDir = normalize(viewPos);
    vec3 reflectDir = reflect(-bhDir, worldNormal);
    float spec = pow(clamp(dot(reflectDir, -viewDir), 0.0, 1.0), 16.0);
    vec3 bhGlint = vec3(1.5, 1.2, 0.8) * spec * clamp(NdotBH, 0.0, 1.0) * (1.0 - blockLight * 0.4);

    float fresnel = pow(clamp(1.0 - abs(normal.z), 0.0, 1.0), 2.5);
    vec3 rimGlow = vec3(0.95, 0.65, 0.35) * (fresnel * 0.35);

    // 4. Ngọn lửa linh hồn tím/lam từ đuốc và đèn
    float torchFactor = pow(blockLight, 2.0);
    vec3 soulTorch = mix(vec3(0.22, 0.88, 1.0), vec3(0.88, 0.35, 1.0), sin(frameTimeCounter * 2.0) * 0.25 + 0.5) * (torchFactor * 1.8);

    // 5. Ánh sáng hư vô thăm thẳm
    vec3 voidAmbient = vec3(0.06, 0.035, 0.05);

    // Tổng hợp ánh sáng
    vec3 totalLight = voidAmbient + cosmicAmbient + bhDirect + bhGlint + soulTorch + rimGlow;

    vec4 vanillaLight = texture2D(lightmap, lmCoord);
    totalLight *= clamp(vanillaLight.rgb * 1.2 + 0.20, 0.30, 1.40);

    vec3 litColor = albedo.rgb * color.rgb * totalLight;

    // 7. SƯƠNG MÙ VŨ TRỤ HƯ VÔ PHÍA XA (Cosmic Nebula Distance Fog)
    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = clamp(1.0 - exp(-pow(dist / (renderDist * 0.68), 2.2)), 0.0, 1.0);
    
    // Sương mù mang sắc tím hư không và cực quang huyền ảo
    vec3 cosmicMist = mix(vec3(0.018, 0.006, 0.042), vec3(0.10, 0.03, 0.18), sin(t * 0.6 + dist * 0.015) * 0.5 + 0.5);

    vec3 finalRgb = mix(litColor, cosmicMist, fogFactor);

    // Sương mù và ánh sáng khi chìm dưới mặt nước (Submerged Abyssal Fog)
    if (isEyeInWater == 1) {
        float waterFogFactor = clamp(1.0 - exp(-dist * 0.055), 0.0, 1.0);
        vec3 underwaterFogColor = vec3(0.012, 0.045, 0.095);
        finalRgb = mix(finalRgb * vec3(0.40, 0.75, 1.15), underwaterFogColor, waterFogFactor);
    }

    gl_FragData[0] = vec4(finalRgb, albedo.a);
}
