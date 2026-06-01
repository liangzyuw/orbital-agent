#!/bin/sh

javac Mobile/*.java
jar cvf Mobile.jar Mobile/*.class
javac -cp Mobile.jar:. *.java