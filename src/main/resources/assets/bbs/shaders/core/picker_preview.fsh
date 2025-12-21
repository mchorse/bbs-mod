#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform int Target;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main()
{
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a < 1.0)
    {
        discard;
    }

    int totalIndex = int(color.r * 255.0) | (int(color.g * 255.0) << 8) | (int(color.b * 255.0) << 16);

    if (totalIndex == Target)
    {
        if (Target >= 7)
        {
            color = vec4(0.0, 0.5, 1.0, 0.5);
        }
        else
        {
            color = vec4(1.0, 1.0, 1.0, 0.5);
        }
    }
    else
    {
        discard;
    }

    fragColor = color * ColorModulator;
}
