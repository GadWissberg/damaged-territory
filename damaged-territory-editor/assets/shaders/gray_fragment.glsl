#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_grayFactor;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);

    // Discard fully transparent pixels
    if (texColor.a < 0.01) {
        discard;
    }

    vec3 gray = vec3(0.5);// neutral gray
    vec3 blended = mix(texColor.rgb, gray, u_grayFactor);

    gl_FragColor = vec4(blended, texColor.a);
}