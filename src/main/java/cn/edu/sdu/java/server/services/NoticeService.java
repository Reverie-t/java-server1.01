package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;

@Service
public class NoticeService {
    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final NoticeRepository noticeRepository;
    private final SystemService systemService;

    public NoticeService(NoticeRepository noticeRepository, SystemService systemService) {
        this.noticeRepository = noticeRepository;
        this.systemService = systemService;

    }
    public Map<String,Object> getMapFromNotice(Notice s) {
        Map<String,Object> m = new HashMap<>();

        if(s == null)
            return m;
        m.put("id",s.getId());
        m.put("title",s.getTitle());
        m.put("time",s.getTime());

        return m;
    }

    //Java 对象的注入 我们定义的这下Java的操作对象都不能自己管理是由有Spring框架来管理的， StudentController 中要使用StudentRepository接口的实现类对象，
    // 需要下列方式注入，否则无法使用， studentRepository 相当于StudentRepository接口实现对象的一个引用，由框架完成对这个引用的赋值，
    // StudentController中的方法可以直接使用

    public List<Map<String,Object>> getNoticeMapList(String title) {
        List<Map<String,Object>> dataList = new ArrayList<>();
        List<Notice> sList = noticeRepository.findNoticeListByTitle(title);  //数据库查询操作
        if (sList == null || sList.isEmpty())
            return dataList;
        for (Notice notice : sList) {
            dataList.add(getMapFromNotice(notice));
        }
        return dataList;
    }

    public DataResponse getNoticeList(DataRequest dataRequest) {
        String title = dataRequest.getString("title");
        List<Map<String,Object>> dataList = getNoticeMapList(title);
        return CommonMethod.getReturnData(dataList);  //按照测试框架规范会送Map的list
    }



    public DataResponse noticeDelete(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");  //获取student_id值
        Notice s = null;
        Optional<Notice> op;
        if (id != null && id > 0) {
            op = noticeRepository.findById(id);   //查询获得实体对象
            if(op.isPresent()) {
                s = op.get();
                //删除对应该学生的账户

                noticeRepository.delete(s);    //首先数据库永久删除学生信息
                  // 然后数据库永久删除学生信息
            }
        }
        return CommonMethod.getReturnMessageOK();  //通知前端操作正常
    }


    public DataResponse getNoticeInfo(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        Notice s = null;
        Optional<Notice> op;
        if (id != null) {
            op = noticeRepository.findById(id); //根据学生主键从数据库查询学生的信息
            if (op.isPresent()) {
                s = op.get();
            }
        }
        return CommonMethod.getReturnData(getMapFromNotice(s)); //这里回传包含学生信息的Map对象
    }

    public DataResponse noticeEditSave(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        Map<String,Object> form = dataRequest.getMap("form"); //参数获取Map对象
        String title = CommonMethod.getString(form, "title");  //Map 获取属性的值
        Notice s = null;

        Optional<Notice> op;
        boolean isNew = false;
        if (id != null) {
            op = noticeRepository.findById(id);  //查询对应数据库中主键为id的值的实体对象
            if (op.isPresent()) {
                s = op.get();
            }
        }

        if (s == null) {

            s = new Notice();   // 创建实体对象
            s.setId(id);
            noticeRepository.saveAndFlush(s);  //插入新的Student记录
            isNew = true;
        }



        s.setTitle(CommonMethod.getString(form, "title"));
        noticeRepository.save(s);  //修改保存学生信息
        systemService.modifyLog(s,isNew);
        return CommonMethod.getReturnData(s.getId());  // 将personId返回前端
    }



}
