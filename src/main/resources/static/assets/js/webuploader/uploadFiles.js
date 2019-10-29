jQuery(function () {
    var $ = jQuery,    // just in case. Make sure it's not an other libaray.
        $wrap = $('#uploader'),
        // 文件容器
        $table = $wrap.find('.queueList'),
        // 状态栏，包括进度和控制按钮
        $statusBar = $wrap.find('.statusBar'),
        // 文件总体选择信息。
        $info = $statusBar.find('.info'),
        // 上传按钮
        $upload = $wrap.find('.uploadBtn'),
        // 没选择文件之前的内容。
        $placeHolder = $wrap.find('.placeholder'),

        // 总体进度条
        $progress = $statusBar.find('.progress').hide(),
        chunkSize = 30 * 1024 * 1024,  //5M
        //文件校验的地址
        checkUrl = '/project/check?projectName=' + projectName,
        //文件上传的地址
        uploadUrl = '/project/doupload?projectName=' + projectName+"&chunkSize="+chunkSize,
        // 添加的文件数量
        fileCount = 0,
        // 添加的文件总大小
        fileSize = 0,
        // 可能有init, ready, uploading, confirm, done.
        state = 'init',
        // 所有文件的进度信息，key为file id
        percentages = {},
        //存储aird对应json的对象，key为file.name
        jsonObj={},

        jsonCount=0,
        // WebUploader实例
        uploader;
    if (!WebUploader.Uploader.support()) {
        alert('Web Uploader 不支持您的浏览器！如果你使用的是IE浏览器，请尝试升级 flash 播放器');
        throw new Error('WebUploader does not support the browser you are using.');
    }
    /*示例json文件*/
    var json=[ {startPtr: 0, endPtr: 56810424}
        ,{startPtr: 56810424, endPtr: 75410465}
        , {startPtr: 75410465, endPtr: 97673547}
        ,{startPtr: 97673547, endPtr: 117781037}
        ,{startPtr: 117781037, endPtr: 138149393}
        , {startPtr: 138149393, endPtr: 159814590}
        , {startPtr: 159814590, endPtr: 179725876}
        ,{startPtr: 179725876, endPtr: 196513879}
        ,{startPtr: 196513879, endPtr: 212836291}
        , {startPtr: 212836291, endPtr: 227681837}
        , {startPtr: 227681837, endPtr: 243311001}
        , {startPtr: 243311001, endPtr: 258137506}
        , {startPtr: 258137506, endPtr: 270656123}
        , {startPtr: 270656123, endPtr: 281842844}
        , {startPtr: 281842844, endPtr: 293299377}
        , {startPtr: 293299377, endPtr: 301459203}
        ,{startPtr: 301459203, endPtr: 309328340}
        ,{startPtr: 309328340, endPtr: 316039919}
        ,{startPtr: 316039919, endPtr: 321240131}
        , {startPtr: 321240131, endPtr: 326463818}
        ,{startPtr: 326463818, endPtr: 331089484}
        , {startPtr: 331089484, endPtr: 334856914}
        , {startPtr: 334856914, endPtr: 338842295}
        , {startPtr: 338842295, endPtr: 342774814}
        , {startPtr: 342774814, endPtr: 345939207}
        , {startPtr: 345939207, endPtr: 348877039}
        , {startPtr: 348877039, endPtr: 351686339}
        , {startPtr: 351686339, endPtr: 354009708}
        ,{startPtr: 354009708, endPtr: 356297974}
        , {startPtr: 356297974, endPtr: 358494242}
        , {startPtr: 358494242, endPtr: 360437121}
        , {startPtr: 360437121, endPtr: 362367650}
        , {startPtr: 362367650, endPtr: 364235695}
    ]
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
    var chunkSize=json.map(function(item){
        return item.endPtr-item.startPtr
    });
    console.log(chunkSize);
    WebUploader.Uploader.register({
        'before-send': 'beforeSend'
    }, {
        beforeSend: function( block ) {
            // console.log(block);
            var task = WebUploader.Deferred();
            var requestData = {
                fileName: block.file.name,
                chunk: block.chunk,
                chunkSize: chunkSize[block.chunk]
            };
            $.ajax({
                type: "POST",
                url: checkUrl,
                data: requestData,
                cache: false,
                async: false, // 同步
                timeout: 1000
            }).then(function (result) {
                console.log(result);
                if (result.msgCode === 'FILE_CHUNK_ALREADY_EXISTED') {
                    task.reject(); // 分片存在，则跳过上传
                } else {
                    task.resolve();
                }
            });
            return task.promise();
        }
    });
    // 实例化
    uploader = WebUploader.create({
        pick: {
            id: '#filePicker',
            label: 'Choose Aird & JSON files'
        },
        formData: {

        },
        accept: {
            title: 'Aird',
            extensions: 'aird,json',
            mimeTypes: '.aird, .json'
        },

        disableGlobalDnd: true,
        chunked: true,
        threads: 1,
        chunkSize: chunkSize,
        server: uploadUrl,
        fileNumLimit: 500,  //一次上传的文件总数目,200个,相当于100个Aird实验(包含100个Aird文件和100个JSON文件)
        fileSizeLimit: 500 * 1024 * 1024 * 1024,    // 200GB
        fileSingleSizeLimit: 10 * 1024 * 1024 * 1024    // 2GB
    });

    // 添加“添加文件”的按钮，
    uploader.addButton({
        id: '#addMoreFile'
    });

    // 当有文件添加进来时执行，负责view的创建
    function addFile(file) {
        var $row = $('<tr id="' + file.id + '">' +
            '<td class="title">' + file.name + '</td>' +
            '<td><div class="progress m--margin-5"><div class="progress-bar progress-bar-striped progress-bar-animated m-progress--lg bg-success" role="progressbar"></div></div></td>' +
            '</tr>'),

            $prgress = $row.find('.progress-bar'),
            $info = $('<td class="error"></td>').appendTo($row),
            $deleteBtn = $('<td><button class="btn btn-sm btn-danger m-btn m-btn--icon m-btn--icon-only"><i class="fa fa-remove"></i></button></td>').appendTo($row),

            showError = function (code) {
                var text;
                switch (code) {
                    case 'exceed_size':
                        text = 'File is too large';
                        break;
                    case 'interrupt':
                        text = 'Upload stop';
                        break;
                    case 'file_existed':
                        text = 'File is already existed';
                        break;
                    case 'server_error':
                        text = 'Server error exception';
                        break;
                    default:
                        text = 'Upload failed, please try again';
                        break;
                }
                $info.text(text);
            };

        if (file.getStatus() === 'invalid') {
            showError(file.statusText);
        } else {
            percentages[file.id] = [file.size, 0];
        }

        file.on('statuschange', function (cur, prev) {

            if (cur === 'error' || cur === 'invalid') {
                showError(file.statusText);
                percentages[file.id][1] = 1;
            } else if (cur === 'interrupt') {
                showError('interrupt');
            } else if (cur === 'queued') {
                percentages[file.id][1] = 0;
            } else if (cur === 'progress') {
                $info.text("processing");
                $prgress.css('display', 'block');
            } else if (cur === 'complete') {
                $info.text("complete");
            }

            $row.removeClass('state-' + prev).addClass('state-' + cur);
        });

        $deleteBtn.on('click', 'button', function () {
            uploader.removeFile(file);
        });

        $row.appendTo($table);
    }

    // 负责view的销毁
    function removeFile(file) {
        var $row = $('#' + file.id);
        delete percentages[file.id];
        updateTotalProgress();
        $row.remove();
    }

    function updateTotalProgress() {
        var loaded = 0,
            total = 0,
            spans = $progress.children(),
            percent;

        $.each(percentages, function (k, v) {
            total += v[0];
            loaded += v[0] * v[1];
        });

        percent = total ? loaded / total : 0;

        spans.eq(0).text(Math.round(percent * 100) + '%');
        spans.eq(1).css('width', Math.round(percent * 100) + '%');
        updateStatus();
    }

    function updateStatus() {
        var text = '', stats;
        if (state === 'ready') {
            text = 'Selected ' + fileCount + ' Files,Totally ' + WebUploader.formatSize(fileSize) + '.';
        } else if (state === 'confirm') {
            stats = uploader.getStats();
            if (stats.uploadFailNum) {
                text = 'Upload ' + stats.successNum + ' files to server success,' +
                    stats.uploadFailNum + ' files failed, <a class="retry" href="#">try again</a> or <a class="ignore" href="#">ignore</a>'
            }
        } else {
            stats = uploader.getStats();
            text = fileCount + ' files totally(' + WebUploader.formatSize(fileSize) + '),' + stats.successNum + ' files success';

            if (stats.uploadFailNum) {
                text += ',' + stats.uploadFailNum + ' files failed';
            }
        }

        $info.html(text);
    }

    function setState(val) {
        var stats;

        if (val === state) {
            return;
        }

        $upload.removeClass('state-' + state);
        $upload.addClass('state-' + val);
        state = val;

        switch (state) {
            case 'init':
                $placeHolder.removeClass('element-invisible');
                $statusBar.addClass('element-invisible');
                uploader.refresh();
                break;

            case 'ready':
                $placeHolder.addClass('element-invisible');
                $('#addMoreFile').removeClass('element-invisible');
                $statusBar.removeClass('element-invisible');
                $upload.removeClass('disabled');
                uploader.refresh();
                break;

            case 'uploading':
                $('#addMoreFile').addClass('element-invisible');
                $progress.show();
                $upload.text('Stop uploading');
                break;

            case 'paused':
                $progress.show();
                $upload.text('Continue to upload');
                break;

            case 'confirm':
                $progress.hide();
                $upload.text('Start upload').addClass('disabled');

                stats = uploader.getStats();
                if (stats.successNum && !stats.uploadFailNum) {
                    setState('finish');
                    return;
                }
                break;
            case 'finish':
                stats = uploader.getStats();
                if (!stats.successNum) {
                    state = 'done';
                    location.reload();
                }
                $upload.text('Start upload').removeClass('disabled');
                $('#addMoreFile').removeClass('element-invisible');
                break;
        }

        updateStatus();
    }

    uploader.onUploadProgress = function (file, percentage) {
        var $row = $('#' + file.id),
            $percent = $row.find('.progress-bar');

        $percent.css('width', percentage * 100 + '%');
        percentages[file.id][1] = percentage;
        updateTotalProgress();
    };

    uploader.onFileQueued = function (file) {
        console.log(file);
        fileCount++;
        fileSize += file.size;
        var fileType=file.name.split(".")[1];
        var reader=new FileReader();
        // reader.readAsBinaryString(file);
        reader.onload=function (ev) {
            // console.log(reader.result);
        };
        if(fileType==="aird"){
            console.log("请求json文件");//换成ajax请求
            var name=file.name;
            jsonObj[name]="json"+jsonCount;
            jsonCount++;
            console.log(jsonObj);
        }
        if(fileType==="json"){
            console.log(file);
        }
        if (fileCount === 1) {
            $placeHolder.addClass('element-invisible');
            $statusBar.show();
        }

        addFile(file);
        setState('ready');
        updateTotalProgress();
    };

    uploader.onFileDequeued = function (file) {
        var name=file.name;
        delete jsonObj[name];
        fileCount--;
        fileSize -= file.size;

        if (!fileCount) {
            setState('init');
        }

        removeFile(file);
        updateTotalProgress();

    };

    uploader.on('all', function (type) {
        switch (type) {
            case 'uploadFinished':
                setState('confirm');
                break;

            case 'startUpload':
                setState('uploading');
                break;

            case 'stopUpload':
                setState('paused');
                break;

        }
    });

    uploader.onError = function (code) {
        alert('Error: ' + code);
    };

    $upload.on('click', function () {
        if ($(this).hasClass('disabled')) {
            return false;
        }
        if (state === 'ready') {
            uploader.upload();
        } else if (state === 'paused') {
            uploader.upload();
        } else if (state === 'uploading') {
            uploader.stop(true);
        }
    });

    $info.on('click', '.retry', function () {
        uploader.retry();
    });

    $info.on('click', '.ignore', function () {
        alert('todo');
    });
    $upload.addClass('state-' + state);
    updateTotalProgress();
});