var fs = require('fs');

console.log(process.argv);

if (process.argv.length < 5) {
    console.log(`Usage: generator.js <Input Collection Folder> <Input Environment File> <Output File Path>`)
    process.exitCode = 1;
    process.exit();
}

var inputFolder = process.argv[2];
var inputEnv = process.argv[3];
var outputFolder = process.argv[4];

function getFiles(dir, files_) {
    files_ = files_ || [];
    var files = fs.readdirSync(dir);
    for (var i in files){
        var name = dir + '/' + files[i];
        if (fs.statSync(name).isDirectory()){
            getFiles(name, files_);
        } else {
            files_.push(name);
        }
    }
    return files_;
}

var environmentFile = JSON.parse(fs.readFileSync(inputEnv, 'utf8'));
var envVars = environmentFile.values;

function replaceEnvVars(text) {
    for(var i = 0; i < envVars.length; i++) {
        text = text.replace(`{{${envVars[i].key}}}`, envVars[i].value);
    }
    return text;
}

index = [];
collections = getFiles(inputFolder);

function generateItemMarkdowns(currentMarkdown, items) {
    for (var j = 0; j < items.length; j++) {
        var example = items[j];

        if (!example.hasOwnProperty('request') && example.hasOwnProperty('item')) {
            currentMarkdown = generateItemMarkdowns(currentMarkdown, example.item);
        }
        else if (!example.name.includes("[nodocs]")) {
            currentMarkdown += `\n## ${example.name}\n`;
            if (example.request.description != undefined) {
                currentMarkdown += `
### Description

${example.request.description}
`
            }

            currentMarkdown +=
`

### Method - **${example.request.method}**
`;
            if (example.request.header.length > 0) {
                currentMarkdown +=
`

### Headers

| Key | Value |
| --- | ----- |
`;
                for (var x = 0; x < example.request.header.length; x++) {
                    var header = example.request.header[x];
                    currentMarkdown += `| ${header.key} | ${header.value} |\n`;
                }
            }

            if (example.request.body[example.request.body.mode].length > 0) {
                currentMarkdown +=
`

### Body Fields - **${example.request.body.mode}**
`;
                for (var x = 0; x < example.request.body[example.request.body.mode].length; x++) {
                    var field = example.request.body[example.request.body.mode][x];
                    currentMarkdown +=
`
#### ${field.key} - _${field.type}_

\`\`\`
${replaceEnvVars(field.value)}
\`\`\`
`;
                }
            }
        }
    }
    return currentMarkdown;
}

for (var i = 0; i < collections.length; i++) {
    console.log(`Reading in - ${collections[i]}`);
    var colls = JSON.parse(fs.readFileSync(collections[i], 'utf8'));
    var markdownOutput =
`# ${colls.info.name}
<!-- toc -->

<!-- tocstop -->
`;
    markdownOutput = generateItemMarkdowns(markdownOutput, colls.item);
    var filePathComponents = collections[i].split("/");
    var markdownFileName = filePathComponents[filePathComponents.length - 1].replace(".json", ".md");
    fs.writeFileSync(`${outputFolder}/${markdownFileName}`, markdownOutput + "\n");
    index.push({
        "name": colls.info.name,
        "collectionFile": collections[i].substr(1),
        "markdownFile": markdownFileName
    })
}

indexFile = `# Examples Directory

The following sets of examples are automatically generated from the included [Postman](https://www.getpostman.com/) collections
`

for (var i = 0; i < index.length; i++) {
    indexFile += `
- [${index[i].name}](${index[i].markdownFile}) - Build from [the following collection](${index[i].collectionFile})`
}

fs.writeFileSync(`${outputFolder}/README.md`, indexFile + "\n");
