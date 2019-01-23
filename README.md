﻿# GenConstants
Generate constant definitions for various programming languages through configuration file.

## Usage
```
usage: java -jar GenConstants [option]
 -h,--help         print help
    --in <arg>     configuration file path
    --lang <arg>   program language
    --out <arg>    constant definition file path
```

## Configuration File Syntax

```
$package=zen.puzzle
$name=Constants

folder_type.normal          =0 #normal folder
folder_type.normal_root     =1 #normal folder root
folder_type.recycle         =2 #recycle folder
folder_type.placeholder     =99 #place holder folder
```
**$package** \
used for java language

**$name** \
generated class name

**constant define format** \
\<group>.\<key>=\<value> #\<comment>

## Other Syntax
### $autoid
```
$autoid=<Initial Value>
```
Items that are not assigned immediately after $autoid line are automatically incremented, 
If encounter an item that has been assigned, it will stop automatically assigning.
```
#Example

$autoid=1

field.env.autologin
field.env.client_id
field.env.client_session_id
field.env.ip
field.env.mac
field.env.machine_name
field.env.session_id
field.env.authcode_cooldown
field.env.authcode

```

## Output Sample
### Java
```
java -jar GenConstants-1.0.0.jar --lang Java --in "D:\Code\github\puzzle\docs\constants.properties" --out "D:\Code\github\puzzle\puzzleserver\src\main\java"
```
File: Constants.java
```Java
package zen.puzzle;

public class Constants {
    public static final int FOLDER_TYPE_NORMAL      = 0;  //normal folder
    public static final int FOLDER_TYPE_NORMAL_ROOT = 1;  //normal folder root
    public static final int FOLDER_TYPE_RECYCLE     = 2;  //recycle folder
    public static final int FOLDER_TYPE_PLACEHOLDER = 99; //place holder folder

}
```

### C++
```
java -jar GenConstants-1.0.0.jar --lang C++ --in "D:\Code\github\puzzle\docs\constants.properties" --out "D:\Code\github\puzzle\puzzle"
```
File: Constants.h
```C++
#pragma once

class Constants
{
public:
    static const int FOLDER_TYPE_NORMAL      = 0;  //normal folder
    static const int FOLDER_TYPE_NORMAL_ROOT = 1;  //normal folder root
    static const int FOLDER_TYPE_RECYCLE     = 2;  //recycle folder
    static const int FOLDER_TYPE_PLACEHOLDER = 99; //place holder folder

};
```
