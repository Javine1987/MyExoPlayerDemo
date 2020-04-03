#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES tex_sampler_0;
uniform sampler2D tex_sampler_1;
uniform float scaleX;
uniform float scaleY;
varying vec2 v_texcoord;

void main() {
    vec4 videoColor = texture2D(tex_sampler_0, v_texcoord);
    vec4 overlayColor = texture2D(tex_sampler_1, vec2(v_texcoord.x * scaleX, v_texcoord.y * scaleY));
    gl_FragColor = videoColor * (1.0 - overlayColor.a) + overlayColor * overlayColor.a;
}
