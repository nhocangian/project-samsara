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

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * color.rgb * light.rgb;

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    vec3 finalColor = mix(vanillaColor, fogColor, fogFactor);

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
