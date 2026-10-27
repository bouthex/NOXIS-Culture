#version 330

// Visión Noxis: el mundo con un tinte amarillo cálido y suave, y los violetas/púrpuras
// mucho más saturados y luminosos. Se calcula sobre la imagen final (sirve para cualquier
// bloque, entidad o partícula, vanilla o de otros mods), sin tocar texturas ni iluminación.

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform NoxisVisionConfig {
    float Warmth;        // cuánto tinte amarillo (0 = nada)
    float VioletBoost;   // cuánto resaltan los violetas (0 = nada)
    float VioletSat;     // multiplicador de saturación de los violetas
    float VioletGlow;    // brillo extra de los violetas
};

out vec4 fragColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    vec3 hsv = rgb2hsv(color);

    // ¿Cuánto violeta es este píxel? Tono entre azul-violeta y magenta, con algo de color y luz.
    float hue = hsv.x * 360.0;
    float hueW = smoothstep(245.0, 268.0, hue) * (1.0 - smoothstep(318.0, 338.0, hue));
    float satW = smoothstep(0.12, 0.32, hsv.y);
    float valW = smoothstep(0.06, 0.20, hsv.z);
    float violet = hueW * satW * valW;

    // Tinte amarillo cálido y sutil: sube un poco rojo/verde y baja el azul, sin quemar los claros.
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 warm = color * vec3(1.0 + 0.10 * Warmth, 1.0 + 0.05 * Warmth, 1.0 - 0.28 * Warmth)
            + vec3(0.05, 0.035, 0.0) * Warmth * (1.0 - luma);
    warm = mix(color, warm, 1.0 - 0.6 * smoothstep(0.85, 1.0, luma));   // los blancos casi no cambian

    // Violetas: mucho más saturados y luminosos (sin perder el detalle de la textura).
    vec3 boosted = hsv2rgb(vec3(hsv.x,
            clamp(hsv.y * VioletSat + 0.12, 0.0, 1.0),
            clamp(hsv.z * (1.0 + VioletGlow) + 0.06 * VioletGlow, 0.0, 1.0)));

    vec3 outColor = mix(warm, boosted, clamp(violet * VioletBoost, 0.0, 1.0));
    fragColor = vec4(clamp(outColor, 0.0, 1.0), 1.0);
}
