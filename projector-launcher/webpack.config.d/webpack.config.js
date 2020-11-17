// todo: remove after https://youtrack.jetbrains.com/issue/KT-35327 is fixed
config.target = 'electron-renderer';

// todo: remove after https://youtrack.jetbrains.com/issue/KT-40159 is fixed
config.output = config.output || {};
config.output.globalObject = "this";
