#version 150

uniform sampler2D Sampler0;

uniform int Target;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a < 0.1)
    {
        discard;
    }

    int totalIndex = Target;
    float r = float(totalIndex & 0xff) / 255.0;
    float g = float((totalIndex >> 8) & 0xff) / 255.0;
    float b = float((totalIndex >> 16) & 0xff) / 255.0;

    fragColor = vec4(r, g, b, 1.0);
}
