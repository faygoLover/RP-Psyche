#version 150

// RP Psyche: выцветание и контраст (как insanity.fsh из Sanity: Descent Into Madness), расхождение цветов к краям
// (chromatical.fsh из того же мода, MIT, (c) 2014 Justin Saunders) и тоннель при панике — одним проходом.

uniform sampler2D DiffuseSampler;
uniform float Desaturate;
uniform float Spread;
uniform float Chroma;
uniform float Tunnel;
uniform float PsyTime;

in vec2 texCoord;

out vec4 fragColor;

vec3 spread(vec3 color, float factor) {
    float mul = 1.0 + ((color.r + color.g + color.b) - 1.0) * factor;
    return color * mul;
}

vec3 desaturate(vec3 color, float factor) {
    vec3 gray = vec3(dot(vec3(0.299, 0.587, 0.114), color));
    return mix(color, gray, factor);
}

void main() {
    vec2 uv = texCoord;
    float d = length(uv - vec2(0.5, 0.5));

    // Цвета расходятся волнами, сильнее к краям.
    float blur = (1.0 + sin(PsyTime * 0.72)) * 0.5;
    blur *= 1.0 + sin(PsyTime * 1.92) * 0.5;
    blur = pow(blur, 3.0) * 0.05 * Chroma * d;

    vec3 col;
    col.r = texture(DiffuseSampler, vec2(uv.x + blur, uv.y)).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, vec2(uv.x - blur, uv.y)).b;
    col *= 1.0 - d * 0.5 * Chroma;

    col = desaturate(spread(col, Spread), Desaturate);
    col *= mix(1.0, 0.91, clamp(Desaturate / 0.7, 0.0, 1.0));

    // Тоннель: края темнеют.
    col *= 1.0 - smoothstep(0.18, 0.72, d) * Tunnel;

    fragColor = vec4(col, 1.0);
}
