const path = require("path");
const nodeExternals = require('webpack-node-externals');

const babelCommon = {
  presets: [
    [
      "@babel/env",
      {
        useBuiltIns: "usage"
      }
    ],
  ],
  plugins: [
    "@babel/proposal-class-properties"
  ]
};

const config = {
    entry: "./app/server",
    mode: "development",
    output: {
      path: path.resolve(__dirname, "dist"),
      filename: "server.js",
      publicPath: "/"
    },
    resolve: {
        extensions: [".ts", ".tsx", ".js", ".json"]
    },
    target: 'node',
    externals: [nodeExternals()],
    node: {
      __dirname: false,
      __filename: false
    },
    // module: {
    //   rules: [
    //     {
    //       // Include ts, tsx, and js files.
    //       test: /\.(tsx?)|(js)$/,
    //       exclude: /node_modules/,
    //       loader: "babel-loader",
    //       options: {
    //         plugins: [...babelCommon.plugins, "babel-plugin-source-map-support"],
    //         presets: [
    //           [
    //             "@babel/env",
    //             {
    //               "targets": {
    //                 "esmodules": false
    //               }
    //             }
         
    //           ]
    //         ]
    //       }
    //     }
    //   ]
    // }
  };
  

module.exports = config;