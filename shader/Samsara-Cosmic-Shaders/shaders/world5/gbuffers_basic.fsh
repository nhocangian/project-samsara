#version 120

/* DRAWBUFFERS:0 */

// Nét tiếng vọng (EchoRenderer): giữ nguyên màu và độ trong.

varying vec4 color;

void main() {
    gl_FragData[0] = color;
}
