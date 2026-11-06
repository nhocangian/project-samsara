#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    vec4 light = texture2D(lightmap, lmCoord);
    gl_FragData[0] = vec4(albedo.rgb * color.rgb * light.rgb, albedo.a);
}
