#version 120

#include "/lib/inception_curve.glsl"

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec4 curvedView = inceptionCurve(gl_ModelViewMatrix * gl_Vertex);
    gl_Position = gl_ProjectionMatrix * curvedView;
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    color = gl_Color;
}
