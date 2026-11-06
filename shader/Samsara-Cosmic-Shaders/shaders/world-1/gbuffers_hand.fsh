#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform mat4 gbufferModelViewInverse;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    float blockLight = clamp((lmCoord.x - 0.03125) / 0.9375, 0.0, 1.0);

    vec3 wNorm = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
    vec3 worldNormal = length(wNorm) > 0.001 ? normalize(wNorm) : vec3(0.0, 1.0, 0.0);

    // Ánh sáng Nether: Nham thạch chiếu từ dưới lên, trần đá đỏ rực từ trên xuống
    float lavaUp = clamp(-worldNormal.y * 0.6 + 0.4, 0.1, 1.0);
    vec3 lavaGlow = vec3(1.0, 0.45, 0.10) * (lavaUp * 0.45);

    float ceilingDown = clamp(worldNormal.y * 0.5 + 0.5, 0.1, 1.0);
    vec3 fogAmbient = vec3(0.32, 0.08, 0.08) * (ceilingDown * 0.5);

    float torchFactor = pow(blockLight, 2.0);
    vec3 torchColor = vec3(1.10, 0.55, 0.15) * (torchFactor * 1.7);

    vec4 vanillaLight = texture2D(lightmap, lmCoord);
    vec3 totalLight = vec3(0.06, 0.03, 0.03) + lavaGlow + fogAmbient + torchColor;
    totalLight *= clamp(vanillaLight.rgb * 1.25 + 0.15, 0.25, 1.4);

    gl_FragData[0] = vec4(albedo.rgb * color.rgb * totalLight, albedo.a);
}
