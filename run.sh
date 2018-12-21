#!/bin/sh

# from one jar and loose classes
java -Xmx64m -cp "bin:lib/core.jar:lib/support.zip" automatype.Automatype $@


# from a single jar
#java -Xmx64m -cp "Automatype.jar" automatype.Automatype $@

