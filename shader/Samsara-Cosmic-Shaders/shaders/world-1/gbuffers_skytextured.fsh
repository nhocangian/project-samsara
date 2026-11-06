#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;

varying vec4 color;
varying vec2 texCoord;

void main() {
    gl_FragData[0] = texture2D(tex, texCoord) * color;
}
