#version 120

/* DRAWBUFFERS:0 */

varying vec4 starColor;

void main() {
    gl_FragData[0] = starColor;
}
