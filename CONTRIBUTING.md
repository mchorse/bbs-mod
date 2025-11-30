# Contribution guide

If you'd like to submit a PR (pull request) to this repository, please carefully read following document. The failure to adhere to this document will result in immediate rejection of the PR.

These rules might feel tyrannical, but these rules ensure that the project stays maintainable for future years! The source code will turn into unmaintainable slop who will never support ever again (these rules weren't enforced in Blockbuster mod, and look what happened: it was abandoned).

## General principles

* No pure AI code (AI code is allowed as long as you adapted it, understood it, and tested it)!
* No changes to `gradle` config (i.e. `gradle/`, `gradlew`, `gradlew.bat`, `gradle.properties`, and `build.gradle`)!

## Code style

Here is a sample of code that adheres to all of the code style rules of this project:

```java
public static void main(String[] args) 
{
    List<String> strings = new ArrayList<>();
    int a = 10;

    /* a is used instead of c, for some reason */
    for (int i = 0; i < a; i++)
    {
        strings.add(String.valueOf(i));
    }
    
    float x = 10F;
    float y = 15.5F;
    float d = findDistance(0F, 0F, x, y);
    
    System.out.println("Distance between " + x + " and " + y + " is " + d + " meters!");
}

/**
 * Given two 2D points, calculate the distance between them
 */
private static float findDistance(float x1, float y1, float x2, float y2)
{
    float dx = x2 - x1;
    float dy = y2 - y1;
    
    return Math.sqrt(dx * dx + dy * dy);
}
```

Here we have:

### Organization

* The structure of any class must have the following order:
    * Fields: constants, static, instance
    * Static constructor
    * Static methods
    * Instance constructors
    * Methods
    * Nested classes, interfaces, enums and records
* Any **dead code** (check for the name being gray in IDE, or references) must be deleted!
* One line constructions are allowed only **if they look very similar** (look harmonious).
* Multiple `if` constructions in a row must be separated with a new line to not mistake it for `else if`s!
* Blocks of variable definitions must be cluttered together (first go objects then primitives, unless it's impossible due to algorithm).
* Blocks of constructions, variable definitions, and method invocation must be separated by two new lines.
* Code repetition/duplication must be avoided! Make sure to check *Utils classes and JOML for any relevant methods.

### Formatting

* `{` is always on the next line.
* All instance method calls and field references must have `this`!
* Try to stay under 150 LOC methods. Refactor accordingly to keep method size small!

### Types

* No generic type provided where can be omitted (i.e. `new ArrayList<>()`).
* Float `1F`, double `2D` and long `3L` number specifiers must be in capital letters always, and if there is no decimal, period must be absent!
* No full references to the classes in the code (i.e. `new org.joml.Vector3f()`, unless there are conflicting names)!
* No `var`!

### Comments

* All comments must be in **English**!
* All comments in the body of the code must be within `/* ... */` and never with `//`.
* Self-explanatory comments must be avoided.
* JavaDocs comments must be present only above the method or class definition, but prior to any **annotations**!