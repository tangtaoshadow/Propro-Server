package com.westlake.air.propro.component;

import com.westlake.air.propro.constants.SymbolConst;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.vo.FileBlockVO;
import com.westlake.air.propro.domain.vo.UploadVO;
import com.westlake.air.propro.utils.FileUtil;
import com.westlake.air.propro.utils.RepositoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Component("chunkUploader")
public class ChunkUploader {

    public final Logger logger = LoggerFactory.getLogger(ChunkUploader.class);

    /**
     * @param block FileBlockVO(fileName=napedro_L120225_010_SW.json, chunk=null, chunkSize=20971520)
     * @return
     * @update tangtao at 2019-11-1 09:49:42
     * @archive 每次上传文件之前的检查 主要负责检查文件是否重复
     */
    public ResultDO check(FileBlockVO block, String projectName) {

        // 文件处理逻辑
        Long chunk = block.getChunk();
        String fileName = block.getFileName();
        Long chunkSize = block.getChunkSize();
        ResultDO result = new ResultDO(true);

        if (chunk != null) {
            // 检查分片是否存在
            // 注意：这里会根据分片位置自动组合出缓存里的文件名称 从而判断是否存在
            String destFileDir = RepositoryUtil.getProjectTempRepo(projectName) + File.separator + fileName;
            String destFileName = chunk + SymbolConst.DELIMITER + fileName;
            String destFilePath = destFileDir + File.separator + destFileName;
            File destFile = new File(destFilePath);
            if (destFile.exists() && destFile.length() == chunkSize) {
                result = ResultDO.buildError(ResultCode.FILE_CHUNK_ALREADY_EXISTED);
            }
        }
        return result;
    }


    /**
     * 分片上传
     *
     * @param file
     * @param uploadVO
     * @return
     */
    public ResultDO chunkUpload(MultipartFile file, UploadVO uploadVO, String projectName) {
        String fileName = uploadVO.getName();
        // 当前片
        Long chunk = uploadVO.getChunk();
        // 总共多少片
        Long chunks = uploadVO.getChunks();

        // 分片目录创建 先往缓存目录存
        String chunkDirPath = RepositoryUtil.getProjectTempRepo(projectName) + File.separator + fileName;
        File chunkDir = new File(chunkDirPath);

        if (!chunkDir.exists()) {
            chunkDir.mkdirs();
        }

        // 分片文件上传
        String chunkFileName = chunk + SymbolConst.DELIMITER + fileName;
        String chunkFilePath = chunkDir + File.separator + chunkFileName;
        File chunkFile = new File(chunkFilePath);

        try {
            if (chunkFile.length() != 0 && file.getSize() == chunkFile.length()) {

            } else {
                // copy 文件
                file.transferTo(chunkFile);
            }

        } catch (Exception e) {
            logger.error("分片上传出错", e);
            return ResultDO.buildError(ResultCode.FILE_CHUNK_UPLOAD_FAILED);
        }

        // 合并分片 其实这个 chunkSize
        Long chunkSize = uploadVO.getChunkSize();
        long seek = 0;
        //
        if (null != chunk) {
            seek = chunkSize * chunk;
        }

        String destFilePath = RepositoryUtil.getProjectRepo(projectName) + File.separator + fileName;
        File destFile = new File(destFilePath);

        if (chunkFile.length() > 0) {
            try {
                // 执行移动
                FileUtil.randomAccessFile(chunkFile, destFile, seek);
            } catch (IOException e) {

                logger.error("分片{}合并失败：{}", chunkFile.getName(), e.getMessage());
                return ResultDO.buildError(ResultCode.FILE_CHUNKS_MERGE_FAILED);
            }
        }

        // 因为chunk是从0开始的 所以当分片传完时 chunk = chunks - 1
        // 如果是 json 那么chunk=null
        if (null == chunk || chunk == chunks - 1) {
            // 删除分片文件
            // FileUtil.deleteDirectory(chunkDirPath);
        }

        return new ResultDO<>(true);
    }
}
