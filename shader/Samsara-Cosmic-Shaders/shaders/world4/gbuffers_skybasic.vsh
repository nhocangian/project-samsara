#version 120

varying vec4 vertexPos;

void main() {
    gl_Position = ftransform();
    vertexPos = gl_Position;
}
