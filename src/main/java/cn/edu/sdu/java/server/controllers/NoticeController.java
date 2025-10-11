package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.NoticeService;
import cn.edu.sdu.java.server.services.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notice")
public class NoticeController {
    private final NoticeService noticeService;
    public NoticeController(NoticeService studentService) {
        this.noticeService = studentService;
    }

    @PostMapping("/getNoticeInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getNoticeInfo(@Valid @RequestBody DataRequest dataRequest) {
        return noticeService.getNoticeInfo(dataRequest);
    }
    @PostMapping("/getNoticeList")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getNoticeList(@Valid @RequestBody DataRequest dataRequest) {
        return noticeService.getNoticeList(dataRequest);
    }


    /**
     * studentDelete 删除学生信息Web服务 Student页面的列表里点击删除按钮则可以删除已经存在的学生信息， 前端会将该记录的id 回传到后端，方法从参数获取id，查出相关记录，调用delete方法删除
     * 这里注意删除顺序，应为user关联person,Student关联Person 所以要先删除Student,User，再删除Person
     *
     * @param dataRequest 前端personId 要删除的学生的主键 person_id
     * @return 正常操作
     */

    @PostMapping("/noticeDelete")
    public DataResponse noticeDelete(@Valid @RequestBody DataRequest dataRequest) {
        return noticeService.noticeDelete(dataRequest);
    }

    /**
     * getStudentInfo 前端点击学生列表时前端获取学生详细信息请求服务
     *
     * @param dataRequest 从前端获取 personId 查询学生信息的主键 person_id
     * @return 根据personId从数据库中查出数据，存在Map对象里，并返回前端
     */
    @PostMapping("/noticeEditSave")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse noticeEditSave(@Valid @RequestBody DataRequest dataRequest) {
        return noticeService.noticeEditSave(dataRequest);
    }
}
