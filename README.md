好的，我会帮你生成完整的README.md文件。这个文件将包含项目说明、结构、配置和使用方法等内容。

```markdown:README.md
# Java Text to Code Converter

这是一个将日语文本转换为Java代码的工具。它可以读取特定格式的日语文本，并生成对应的Java setter方法调用代码。

## 功能特点

- 支持从配置文件读取输入/输出路径
- 支持多种日语文本格式（通过策略模式扩展）
- 智能匹配实体类中的字段
- 支持半角/全角字符转换
- 支持日文假名的标准化处理

## 项目结构

```
project_root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── TextToJavaConverter.java
│   │   │           ├── config/
│   │   │           ├── factory/
│   │   │           ├── strategy/
│   │   │           └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── ...
├── input/
│   ├── t1.txt
│   └── TestTable1BaseEntity.java
└── output/
    └── GeneratedCode.java
```

## 配置说明

在 `application.yml` 中配置：

```yaml
converter:
  inputFile: input/t1.txt              # 输入文件路径
  entityFile: input/TestTable1BaseEntity.java  # 实体类文件路径
  outputFile: output/GeneratedCode.java # 输出文件路径
  genType: 1                           # 转换器类型
```

## 使用方法

1. 准备输入文件
   - 在input目录下创建输入文本文件（例如：t1.txt）
   - 文件内容示例：
   ```
   項目「手袋(Ｌ０１).(生活／仕事　会社コード)」にブランクを代入します。
   項目「手袋(Ｌ０１).(請求 情報)」＝　０。
   ```

2. 准备实体类文件
   - 在input目录下放置实体类文件
   - 确保实体类中的注释包含对应的日语说明

3. 运行程序
   ```bash
   mvn clean package
   mvn exec:java
   mvn exec:java -Dexec.args="type2"
   ```

## 开发指南

### Maven命令

```bash
# 清理并安装
mvn clean install

# 运行测试
mvn test

# 编译
mvn compile

# 打包并跳过测试
mvn clean package -DskipTests

# 运行程序
mvn exec:java
```


### 添加新的转换器

1. 创建新的转换器类：
```java
public class Type2TextConverter implements TextConverter {
    @Override
    public String convertLine(String line, List<String> entityLines) {
        // 实现转换逻辑
    }
}
```

2. 在ConverterFactory中注册：
```java
case 2:
    return new Type2TextConverter();
```

## 注意事项

1. 所有文件必须使用UTF-8编码
2. 日语文本中的空格和特殊字符会被正确处理
3. 支持半角/全角字符的自动转换
4. 确保实体类的注释格式正确

## 依赖项

- Java 11+
- Maven 3.6+
- SLF4J + Logback
- JUnit 4
- SnakeYAML

## 输入文本格式说明

支持的文本格式示例：
```
項目「手袋(Ｌ０１).(生活／仕事　会社コード)」にブランクを代入します。
項目「手袋(Ｌ０１).(生活／仕事　取引先コード)」にブランク　を代入します。
項目「手袋(Ｌ０１).(請求　時間)」＝　０。
```

生成的代码示例：
```java
testTable1BaseEntity.setTestField1("");
testTable1BaseEntity.setTestField2("");
testTable1BaseEntity.setTestField6(0);
```

## 许可证

MIT License

## 问题反馈

如有问题，请在GitHub Issues中提出。
```

这个README.md文件包含了：
1. 项目介绍和功能说明
2. 详细的项目结构
3. 配置和使用方法
4. 开发指南和命令说明
5. 注意事项和依赖说明
6. 输入输出格式示例

需要我对某部分做进一步补充或修改吗？
