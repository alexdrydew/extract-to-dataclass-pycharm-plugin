# Extract to Dataclass

[![PyCharm Plugin](https://img.shields.io/jetbrains/plugin/v/24800-extract-to-dataclass.svg)](https://plugins.jetbrains.com/plugin/24800-extract-to-dataclass)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/alexdrydew/ExtractToDataclass/check.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A PyCharm plugin that enables refactoring of function arguments into separate dataclasses.

## Example

Before refactoring:

```python
def process_user(name: str, age: int, email: str, is_active: bool = True):
    print(f"Processing user: {name}, {age}, {email}, {is_active}")
```

After refactoring:

```python
from dataclasses import dataclass

@dataclass
class UserData:
    name: str
    age: int
    email: str
    is_active: bool = True

def process_user(user_data: UserData):
    print(f"Processing user: {user_data.name}, {user_data.age}, {user_data.email}, {user_data.is_active}")
```

## Overview

This plugin introduces an "Extract to Dataclass" refactoring action, allowing you to quickly organize function
parameters into structured dataclasses. It's designed to enhance your code's readability and maintainability in Python
projects.

## Key Features

- 🚀 Extract selected function parameters into a new dataclass
- 🔍 Type-friendly: preserve existing type annotations, including generic types
- 🔄 Automatically update function calls and parameter usages
- 📦 Handle dataclass imports and type annotations

## Installation

The plugin requires PyCharm 2023.1 or later.

1. Open PyCharm
2. Navigate to `Settings/Preferences` → `Plugins`
3. Search for "Extract to Dataclass"
4. Click `Install`

## Usage

1. Select function that requires refactoring
2. Access via right-click `Refactor` menu
3. Select `Extract to Dataclass`
4. Choose parameters to extract
5. Specify dataclass and parameter names (or use defaults)
6. Confirm to perform the refactoring

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
