/*
updateTime 2019-11-9 22:17:40
updateAuthor tangtao 2019-11-9 14:06:36
*/

$(function() {
  var $ = jQuery, // just in case. Make sure it's not an other libaray.
    $wrap = $("#uploader"),
    // 文件容器
    $table = $wrap.find(".queueList"),
    // 状态栏，包括进度和控制按钮
    $statusBar = $wrap.find(".statusBar"),
    // 文件总体选择信息。
    $info = $statusBar.find(".info"),
    // 上传按钮
    $upload = $wrap.find(".uploadBtn"),
    // 没选择文件之前的内容。
    $placeHolder = $wrap.find(".placeholder"),
    // 总体进度条
    $progress = $statusBar.find(".progress").hide(),
    // 默认 3GB 不分片
    defaultChunkSize = [3000 * 1024 * 104],
    chunkSize = defaultChunkSize, // 3000M 默认不分片
    //文件校验的地址
    checkUrl = "/project/check?projectName=" + projectName,
    tao = {
      // jsonn 文件名字
      jsonFileName: "",
      jsonFileNameStatus: -1,
      // json 描述文件分片
      jsonFileList: [],
      jsonFileListStatus: -1
    },
    // 指明当前上传的 aird 文件使用的是哪个 json 进行分割的
    // 文件已经上传大小
    uploadSize = 0,
    // 文件大小
    //文件上传的地址
    uploadUrl =
      "/project/doupload?projectName=" +
      projectName +
      "&chunkSize=" +
      chunkSize,
    // 添加的文件数量
    fileCount = 0,
    // 添加的文件总大小
    fileSize = 0,
    // 可能有init, ready, uploading, confirm, done.
    state = "init",
    // 所有文件的进度信息，key为file id
    percentages = {},
    //存储aird对应json的对象，key为file.name
    jsonObj = {},
    jsonCount = 0,
    // WebUploader实例
    uploader;
  if (!WebUploader.Uploader.support()) {
    alert(
      "Web Uploader 不支持您的浏览器！如果你使用的是IE浏览器，请尝试升级 flash 播放器"
    );
    throw new Error("WebUploader does not support the browser you are using.");
  }
  console.log("========");

  /*json文件获取请求*/
  // $.ajax({
  //     type: "POST",
  //     url: checkUrl,
  //     data: requestData,
  //     cache: false,
  //     async: false, // 同步
  //     timeout: 1000
  // }).then(function (data) {
  //     var json=data;
  // });
  // chunkSize 尝试获取 分片数组 指定切分位置
  chunkSize = JSON.parse(window.localStorage.getItem("jsonFileList"));
  console.log("chunkSize==", chunkSize);
  /***
   * 上传文件初始化校验
   *
   *
   */

  tao_init = () => {
    let jsonFileName0 = window.localStorage.getItem("jsonFileName");
    let jsonFileList0 = window.localStorage.getItem("jsonFileList");

    // 给出提示
    let str = `
    文件上传步骤
    <br/>
    (1)每次上传aird文件前,请先上传json文件
    <br/>
    (2)接下来上传json配置好的aird文件
    <br/>
    (3)json文件可以直接上传
    `;
    print_info(str);

    let res = -1;
    try {
      if ("" != jsonFileList0 && "" != jsonFileName0) {
        jsonFileList0 = JSON.parse(jsonFileList0);
        // 显示参数
        tao.jsonFileName = jsonFileName0;
        tao.jsonFileNameStatus = 0;
        tao.jsonFileList = jsonFileList0;
        tao.jsonFileListStatus = 0;
        let size = 0;
        for (let i = 0, len = jsonFileList0.length; i < len; i++) {
          size += jsonFileList0[i];
        }
        size = size / 1024 / 1024;
        str = `
        <br/>
        json文件已经配置完成,可以上传aird文件<br/>
        当前json文件为：${tao.jsonFileName}<br/>
        描述信息:<br/>
        aird文件大小为(MB)：${parseFloat(size)}<br/>
        aird文件分片数量：${jsonFileList0.length}<br/>
        aird文件分片信息：${jsonFileList0}<br/>
        `;
        print_info(str);
        res = 0;
      }
    } catch (e) {
      res = -2;
    }
    console.log(res);
  };
  setTimeout(() => {
    tao_init();
  }, 300);
  WebUploader.Uploader.register(
    {
      "before-send": "beforeSend"
    },
    {
      beforeSend: block => {
        // 注册发送前校验 主要目的判断文件有没有已经存在
        console.log("beforeSend", block);
        console.log(chunkSize);
        var task = WebUploader.Deferred();
        let requestData = {};

        let res = -1;
        // 判断是否是 aird 文件
        let filename = block.file.name;

        //
        let sendUrl = checkUrl;
        do {
          if (filename.endsWith(".aird")) {
            // 发送aird前设置传输参数
            let res0 = sendAirdInit(filename);
            if (0 == res0) {
              // success
              res = 0;
              try {
                requestData = {
                  // 文件名称
                  fileName: block.file.name,
                  // 表示当前的分片位置
                  chunk: block.chunk,
                  // 即将上传的文件大小
                  chunkSize: chunkSize[block.chunk]
                };
              } catch (e) {
                requestData = {
                  // 文件名称
                  fileName: block.file.name,
                  // 表示当前的分片位置
                  chunk: 0,
                  // 即将上传的文件大小
                  chunkSize: defaultChunkSize
                };
              }
              break;
            } else {
              console.error("终止传输");
              res = -2;
              break;
            }
          } else if (filename.endsWith(".json")) {
            res = 0;
            requestData = {
              // 文件名称
              fileName: block.file.name,
              // 表示当前的分片位置
              chunk: 0,
              // 即将上传的文件大小
              chunkSize: defaultChunkSize
            };
            // json有专门的上传校验文档
            // sendUrl = `/project/checkFile?fileName=${block.file.name}&projectName=${projectName}`;
            sendUrl = `/project/checkFile?projectName=${projectName}`;
          } else {
            //
          }
        } while (0);

        if (0 != res) {
          // 拒绝
          return task.reject();
        } else {
          let str = `<br/>正在上传${filename}
          <br/>当前片${block.chunk}
          <br/>总共片数${block.chunks}
          <br/>分片位置${block.start}->${block.end}
          <br/>分片长度${block.end - block.start}`;
          print_info(str);
        }

        console.log("每次发送前调用 包括上传json文件");
        $.ajax({
          type: "POST",
          url: sendUrl,
          data: requestData,
          cache: false,
          async: false, // 同步
          timeout: 1000
        }).then(result => {
          // 判断是否需要继续上传文件
          console.log("服务器返回数据", result);
          if (result.msgCode === "FILE_CHUNK_ALREADY_EXISTED") {
            // 分片存在，则跳过上传
            console.log("分片存在，则跳过上传");
            return task.reject();
          } else {
            console.log("可以上传");
            try {
              if (-3 == result.status) {
                // 文件不存在 可以上传
                return task.resolve();
              } else {
                let str = `<span style="color:#dc3545;">.文件已经存在 不允许上传</span>`;
                console.log("文件已经存在 不允许上传");
                print_info(str);
                return task.reject();
              }
            } catch (e) {}

            return task.resolve();
          }
        });
        return task.promise();
      }
    }
  );
  // 实例化
  uploader = WebUploader.create({
    pick: {
      id: "#filePicker",
      label: "Choose Aird & JSON files"
    },
    formData: {},
    accept: {
      title: "Aird",
      extensions: "aird,json",
      mimeTypes: ".aird, .json"
    },
    disableGlobalDnd: true,
    chunked: true,
    threads: 1,
    // 如果为空 则使用不分片
    // chunkSize: null == chunkSize ? 3000 * 1024 * 104 : chunkSize,
    chunkSize: null == chunkSize ? defaultChunkSize : chunkSize,
    server: uploadUrl,
    fileNumLimit: 500, //一次上传的文件总数目,200个,相当于100个Aird实验(包含100个Aird文件和100个JSON文件)
    fileSizeLimit: 500 * 1024 * 1024 * 1024, // 200GB
    fileSingleSizeLimit: 10 * 1024 * 1024 * 1024 // 2GB
  });

  // 添加“添加文件”的按钮，
  uploader.addButton({
    id: "#addMoreFile"
  });

  sendAirdInit = filename => {
    // 先判断文件分片是否在
    //  把后缀 .aird 去掉
    console.log("sendAirdInit");
    // 替换 后缀名为 .json
    filename = filename.substring(0, filename.lastIndexOf(".aird")) + ".json";
    if (tao.jsonFileName == filename) {
      // 说明已经存在
      console.log("说明已经存在");
      return 0;
    } else {
      // 先发起同步请求json数据 以 filename 为准 因为他可以变 aird可以任意上传

      print_info("向服务器请求json数据,json文件名:" + filename);
      console.log("向服务器索取json数据 filename", filename);
      getJsonFile(filename);
    }

    return -1;
  };

  print_info = (str = "", status = 0) => {
    let str0 = "";
    str += "<br/>";
    if (0 == status) {
      // 追加
      str0 = $("#tao-info").html();
      str0 = str + str0;
    } else {
      // 替换
      str0 = str;
    }
    $("#tao-info").html(str0);
  };

  // 向服务器获取 json文件数据
  getJsonFile = filename => {
    console.log("getJsonFile--filename", filename);
    let url = "/project/readJsonFile";
    // 配置post 请求参数
    let requestData = {
      projectName,
      fileName: filename
    };
    console.log("请求内容", requestData);
    $.ajax({
      type: "POST",
      url: url,
      data: requestData,
      cache: false,
      async: true, // 同步
      timeout: 1000
    }).then(function(data) {
      print_info("服务器返回json数据");
      console.log("服务器返回 json file", data);
      let obj = { status: -1 };
      try {
        obj = JSON.parse(data);
      } catch (e) {
        console.log(e);
        obj = { status: -1 };
      }

      // 判断是否 成功 成功就写入数据
      if (0 == obj.status) {
        print_info("上传参数准备成功，可以上传");
        window.localStorage.setItem("jsonFileName", obj.data.jsonFileName);
        window.localStorage.setItem(
          "jsonFileList",
          JSON.stringify(obj.data.fileFragmentList)
        );
        tao.jsonFileName = obj.data.jsonFileName;
        tao.jsonFileNameStatus = 0;
        tao.jsonFileList = obj.data.fileFragmentList;
        chunkSize = tao.jsonFileList;
        let k = 0;
        for (let i = 0; i < chunkSize.length; i++) {
          k += chunkSize[i];
          console.log("i=" + i, k);
        }
        tao.jsonFileListStatus = 0;
        console.log("写入 tao", tao);
      } else {
        console.error("tangtao 服务器返回数据出错", data);
      }
    });
  };

  // 文件添加时回调此函数
  uploader.onFileQueued = function(file) {
    console.log("onFileQueued", file);
    fileCount++;
    fileSize += file.size;
    var fileType = file.name.split(".")[1];
    var reader = new FileReader();
    // reader.readAsBinaryString(file);
    reader.onload = function(ev) {
      // console.log(reader.result);
    };
    if (fileType == "aird") {
      console.log("先判断json数据是否存在，没有就先发起请求json数据");
      var name = file.name;
      console.log(name);
      // 不管是否存在 都发送
      sendAirdInit(name);
      jsonObj[name] = jsonCount;
      jsonCount++;
    }
    let obj = {
      filename: file.name,
      filesize: fileSize,
      lastModifiedDate: file.lastModifiedDate,
      id: file.id,
      ext: file.ext
    };
    if (fileType == "json") {
      console.log("111文件类型是 json", file);
      let str = `
      可以直接上传<br/>
      上传的文件类型是json<br/>
      文件信息:${JSON.stringify(obj)}<br/>
      `;
      print_info(str);
    }
    if (fileCount === 1) {
      $placeHolder.addClass("element-invisible");
      $statusBar.show();
    }

    addFile(file);
    setState("ready");
    updateTotalProgress();
  };

  //
  // 当有文件添加进来时执行，负责view的创建
  function addFile(file) {
    var $row = $(
        '<tr id="' +
          file.id +
          '">' +
          '<td class="title">' +
          file.name +
          "</td>" +
          '<td><div class="progress m--margin-5"><div class="progress-bar progress-bar-striped progress-bar-animated m-progress--lg bg-success" role="progressbar"></div></div></td>' +
          "</tr>"
      ),
      $prgress = $row.find(".progress-bar"),
      $info = $('<td class="error"></td>').appendTo($row),
      $deleteBtn = $(
        '<td><button class="btn btn-sm btn-danger m-btn m-btn--icon m-btn--icon-only"><i class="fa fa-remove"></i></button></td>'
      ).appendTo($row),
      showError = function(code) {
        var text;
        switch (code) {
          case "exceed_size":
            text = "File is too large";
            break;
          case "interrupt":
            text = "Upload stop";
            break;
          case "file_existed":
            text = "File is already existed";
            break;
          case "server_error":
            text = "Server error exception";
            break;
          default:
            text = "Upload failed, please try again";
            break;
        }
        $info.text(text);
      };

    if (file.getStatus() === "invalid") {
      showError(file.statusText);
    } else {
      percentages[file.id] = [file.size, 0];
    }

    file.on("statuschange", function(cur, prev) {
      if (cur === "error" || cur === "invalid") {
        showError(file.statusText);
        percentages[file.id][1] = 1;
      } else if (cur === "interrupt") {
        showError("interrupt");
      } else if (cur === "queued") {
        percentages[file.id][1] = 0;
      } else if (cur === "progress") {
        $info.text("processing");
        $prgress.css("display", "block");
      } else if (cur === "complete") {
        $info.text("complete");
      }

      $row.removeClass("state-" + prev).addClass("state-" + cur);
    });

    $deleteBtn.on("click", "button", function() {
      uploader.removeFile(file);
    });

    $row.appendTo($table);
  }

  // 负责view销毁
  function removeFile(file) {
    var $row = $("#" + file.id);
    delete percentages[file.id];
    updateTotalProgress();
    $row.remove();
  }

  function updateTotalProgress() {
    var loaded = 0,
      total = 0,
      spans = $progress.children(),
      percent;

    $.each(percentages, function(k, v) {
      total += v[0];
      loaded += v[0] * v[1];
    });

    percent = total ? loaded / total : 0;

    spans.eq(0).text(Math.round(percent * 100) + "%");
    spans.eq(1).css("width", Math.round(percent * 100) + "%");
    updateStatus();
  }

  function updateStatus() {
    var text = "",
      stats;
    if (state === "ready") {
      text =
        "Selected " +
        fileCount +
        " Files,Totally " +
        WebUploader.formatSize(fileSize) +
        ".";
    } else if (state === "confirm") {
      stats = uploader.getStats();
      if (stats.uploadFailNum) {
        text =
          "Upload " +
          stats.successNum +
          " files to server success," +
          stats.uploadFailNum +
          ' files failed, <a class="retry" href="#">try again</a> or <a class="ignore" href="#">ignore</a>';
      }
    } else {
      stats = uploader.getStats();
      text =
        fileCount +
        " files totally(" +
        WebUploader.formatSize(fileSize) +
        ")," +
        stats.successNum +
        " files success";

      if (stats.uploadFailNum) {
        text += "," + stats.uploadFailNum + " files failed";
      }
    }

    $info.html(text);
  }

  function setState(val) {
    var stats;

    if (val === state) {
      return;
    }

    $upload.removeClass("state-" + state);
    $upload.addClass("state-" + val);
    state = val;

    switch (state) {
      case "init":
        $placeHolder.removeClass("element-invisible");
        $statusBar.addClass("element-invisible");
        uploader.refresh();
        break;

      case "ready":
        $placeHolder.addClass("element-invisible");
        $("#addMoreFile").removeClass("element-invisible");
        $statusBar.removeClass("element-invisible");
        $upload.removeClass("disabled");
        uploader.refresh();
        break;

      case "uploading":
        $("#addMoreFile").addClass("element-invisible");
        $progress.show();
        $upload.text("Stop uploading");
        break;

      case "paused":
        $progress.show();
        $upload.text("Continue to upload");
        break;

      case "confirm":
        $progress.hide();
        $upload.text("Start upload").addClass("disabled");

        stats = uploader.getStats();
        if (stats.successNum && !stats.uploadFailNum) {
          setState("finish");
          return;
        }
        break;
      case "finish":
        stats = uploader.getStats();
        if (!stats.successNum) {
          state = "done";
          location.reload();
        }
        $upload.text("Start upload").removeClass("disabled");
        $("#addMoreFile").removeClass("element-invisible");
        break;
    }

    updateStatus();
  }

  uploader.onUploadProgress = function(file, percentage) {
    console.log("onUploadProgress");
    var $row = $("#" + file.id),
      $percent = $row.find(".progress-bar");

    $percent.css("width", percentage * 100 + "%");
    percentages[file.id][1] = percentage;
    updateTotalProgress();
  };

  uploader.onFileDequeued = function(file) {
    var name = file.name;
    delete jsonObj[name];
    fileCount--;
    fileSize -= file.size;

    if (!fileCount) {
      setState("init");
    }

    removeFile(file);
    updateTotalProgress();
  };

  uploader.on("all", function(type) {
    switch (type) {
      case "uploadFinished":
        setState("confirm");
        break;

      case "startUpload":
        setState("uploading");
        break;

      case "stopUpload":
        setState("paused");
        break;
    }
  });

  uploader.onError = function(code) {
    alert("Error: " + code);
  };

  $upload.on("click", function() {
    if ($(this).hasClass("disabled")) {
      return false;
    }
    if (state === "ready") {
      uploader.upload();
    } else if (state === "paused") {
      uploader.upload();
    } else if (state === "uploading") {
      uploader.stop(true);
    }
  });

  $info.on("click", ".retry", function() {
    uploader.retry();
  });

  $info.on("click", ".ignore", function() {
    alert("todo");
  });
  $upload.addClass("state-" + state);
  updateTotalProgress();
});
