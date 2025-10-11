package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentLeave;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.StudentLeaveRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class StudentLeaveService {
    private static final Logger logger = LoggerFactory.getLogger(StudentLeaveService.class);
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentLeaveRepository studentLeaveRepository;

    public StudentLeaveService(StudentRepository studentRepository, TeacherRepository teacherRepository, StudentLeaveRepository studentLeaveRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentLeaveRepository = studentLeaveRepository;
    }

    public OptionItemList getTeacherItemOptionList(DataRequest dataRequest) {
        List<Teacher> sList = teacherRepository.findAll();  //数据库查询操作
        List<OptionItem> itemList = new ArrayList<>();
        for (Teacher t : sList) {
            itemList.add(new OptionItem(t.getPersonId(), t.getPersonId() + "", t.getPerson().getNum() + "-" + t.getPerson().getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getStudentLeaveList(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        String userName = CommonMethod.getUsername();
        Integer state = dataRequest.getInteger("state");
        if(state == null)
            state = -1;
        String search = dataRequest.getString("search");
        assert roleName != null;
        List<StudentLeave> slList = switch (roleName) {
            case "ROLE_STUDENT" -> studentLeaveRepository.getStudentLeaveList(-1, search, userName, "");
            case "ROLE_TEACHER" -> studentLeaveRepository.getStudentLeaveList(-1, search, "", userName);
            case "ROLE_ADMIN" -> studentLeaveRepository.getStudentLeaveList(state, search, "", "");
            default -> null;
        };
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> map;
        Student s;
        Teacher t;
        ComDataUtil di = ComDataUtil.getInstance();
        if (slList != null && !slList.isEmpty()) {
            for (StudentLeave sl : slList) {
                map = new HashMap<>();
                s = sl.getStudent();
                t = sl.getTeacher();
                map.put("studentLeaveId", sl.getStudentLeaveId());
                map.put("studentNum", s.getPerson().getNum());
                map.put("studentName", s.getPerson().getName());
                map.put("studentId", s.getPersonId());
                map.put("teacherName", t.getPerson().getNum() + t.getPerson().getName());
                map.put("state", sl.getState());
                map.put("stateName", di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
                map.put("reason", sl.getReason());
                map.put("leaveDate", sl.getLeaveDate());
                map.put("adminComment", sl.getAdminComment());
                map.put("teacherId", t.getPersonId());
                map.put("teacherComment", sl.getTeacherComment());
                dataList.add(map);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse studentLeaveSave(DataRequest dataRequest) {
        logger.info("开始处理学生请假申请保存请求");
        try {
            // 获取请求参数
            Integer state = dataRequest.getInteger("state");
            Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
            Integer teacherId = dataRequest.getInteger("teacherId");
            String leaveDate = dataRequest.getString("leaveDate");
            String reason = dataRequest.getString("reason");
            
            logger.info("请求参数 - state: {}, studentLeaveId: {}, teacherId: {}, leaveDate: {}, reason: {}", 
                       state, studentLeaveId, teacherId, leaveDate, reason);
            
            // 验证必填参数
            if (leaveDate == null || leaveDate.trim().isEmpty()) {
                logger.warn("请假日期为空");
                return CommonMethod.getReturnMessageError("请假日期不能为空");
            }
            if (reason == null || reason.trim().isEmpty()) {
                logger.warn("请假原因为空");
                return CommonMethod.getReturnMessageError("请假原因不能为空");
            }
            
            StudentLeave sl = null;
            
            // 如果是更新现有请假申请
            if(studentLeaveId != null && studentLeaveId > 0) {
                logger.info("更新现有请假申请，ID: {}", studentLeaveId);
                Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
                if(op.isPresent()) {
                    sl = op.get();
                    logger.info("找到现有请假申请，当前状态: {}", sl.getState());
                    // 验证权限：只有待审核状态(0)才能修改
                    if(sl.getState() != 0) {
                        logger.warn("请假申请状态不允许修改，当前状态: {}", sl.getState());
                        return CommonMethod.getReturnMessageError("只有待审核状态的请假申请才能修改");
                    }
                } else {
                    logger.warn("未找到ID为{}的请假申请", studentLeaveId);
                }
            }
            
            // 如果是新建请假申请
            if(sl == null) {
                logger.info("创建新的请假申请");
                sl = new StudentLeave();
                sl.setState(0);
                sl.setApplyTime(new Date());
                sl.setTeacherComment("");
                sl.setAdminComment("");
                
                // 获取当前学生信息
                String username = CommonMethod.getUsername();
                logger.info("当前用户名: {}", username);
                if (username == null || username.trim().isEmpty()) {
                    logger.error("无法获取当前用户信息");
                    return CommonMethod.getReturnMessageError("无法获取当前用户信息，请重新登录");
                }
                
                Optional<Student> studentOpt = studentRepository.findByPersonNum(username);
                if(studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    sl.setStudent(student);
                    logger.info("成功关联学生信息，学生ID: {}, 姓名: {}", 
                               student.getPersonId(), student.getPerson().getName());
                } else {
                    logger.error("未找到用户名为{}的学生信息", username);
                    return CommonMethod.getReturnMessageError("未找到当前用户对应的学生信息，请检查用户数据");
                }
            }
            
            // 设置教师信息（如果提供了teacherId）
            if(teacherId != null && teacherId > 0) {
                logger.info("设置教师信息，教师ID: {}", teacherId);
                Optional<Teacher> op = teacherRepository.findById(teacherId);
                if(op.isPresent()) {
                    Teacher teacher = op.get();
                    sl.setTeacher(teacher);
                    logger.info("成功关联教师信息，教师姓名: {}", teacher.getPerson().getName());
                } else {
                    logger.error("未找到ID为{}的教师", teacherId);
                    return CommonMethod.getReturnMessageError("指定的教师不存在");
                }
            }
            
            // 设置请假信息
            sl.setLeaveDate(leaveDate);
            sl.setReason(reason);
            if (state != null) {
                sl.setState(state);
            }
            
            logger.info("准备保存请假申请 - 请假日期: {}, 请假原因: {}, 状态: {}", 
                       sl.getLeaveDate(), sl.getReason(), sl.getState());
            
            // 保存请假申请
            StudentLeave savedLeave = studentLeaveRepository.save(sl);
            logger.info("请假申请保存成功，ID: {}", savedLeave.getStudentLeaveId());
            
            return CommonMethod.getReturnMessageOK("请假申请保存成功");
            
        } catch (Exception e) {
            // 记录详细错误信息
            logger.error("保存请假申请时发生错误", e);
            return CommonMethod.getReturnMessageError("保存请假申请失败: " + e.getMessage());
        }
    }
    public DataResponse studentLeaveCheck(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String teacherComment = dataRequest.getString("teacherComment");
        String adminComment = dataRequest.getString("adminComment");
        StudentLeave sl = null;
        if(studentLeaveId != null && studentLeaveId > 0) {
            Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
            if(op.isPresent())
                sl = op.get();
        }
        if(sl == null) {
            return CommonMethod.getReturnMessageOK();
        }
        if("ROLE_ADMIN".equals(roleName)) {
            sl.setAdminComment(adminComment);
            sl.setAdminTime(new Date());
            sl.setState(state+2);
        } else if("ROLE_TEACHER".equals(roleName)) {
            sl.setTeacherComment(teacherComment);
            sl.setTeacherTime(new Date());
            sl.setState(state);
        }
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }

    // 学生专用方法实现
    public DataResponse getMyLeaveList(DataRequest dataRequest) {
        String userName = CommonMethod.getUsername();
        Integer state = dataRequest.getInteger("state");
        if(state == null)
            state = -1;
        String search = dataRequest.getString("search");
        Integer currentPage = dataRequest.getCurrentPage();
        Integer pageSize = dataRequest.getInteger("pageSize");
        if(pageSize == null || pageSize <= 0)
            pageSize = 10;
        
        List<StudentLeave> slList = studentLeaveRepository.getStudentLeaveList(state, search, userName, "");
        
        // 分页处理
        int total = slList.size();
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, total);
        List<StudentLeave> pagedList = slList.subList(start, end);
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> map;
        Student s;
        Teacher t;
        ComDataUtil di = ComDataUtil.getInstance();
        
        for (StudentLeave sl : pagedList) {
            map = new HashMap<>();
            s = sl.getStudent();
            t = sl.getTeacher();
            map.put("studentLeaveId", sl.getStudentLeaveId());
            map.put("studentNum", s.getPerson().getNum());
            map.put("studentName", s.getPerson().getName());
            map.put("studentId", s.getPersonId());
            map.put("teacherName", t.getPerson().getName());
            map.put("teacherNum", t.getPerson().getNum());
            map.put("state", sl.getState());
            map.put("stateName", di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
            map.put("reason", sl.getReason());
            map.put("leaveDate", sl.getLeaveDate());
            map.put("adminComment", sl.getAdminComment());
            map.put("teacherComment", sl.getTeacherComment());
            map.put("applyTime", sl.getApplyTime());
            map.put("teacherTime", sl.getTeacherTime());
            map.put("adminTime", sl.getAdminTime());
            dataList.add(map);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", dataList);
        result.put("total", total);
        result.put("currentPage", currentPage + 1);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);
        
        return CommonMethod.getReturnData(result);
    }

    public DataResponse getLeaveDetail(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String userName = CommonMethod.getUsername();
        
        if(studentLeaveId == null || studentLeaveId <= 0) {
            return CommonMethod.getReturnMessageError("请假申请ID不能为空");
        }
        
        Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
        if(!op.isPresent()) {
            return CommonMethod.getReturnMessageError("请假申请不存在");
        }
        
        StudentLeave sl = op.get();
        // 验证是否为当前学生的请假申请
        if(!sl.getStudent().getPerson().getNum().equals(userName)) {
            return CommonMethod.getReturnMessageError("无权限查看此请假申请");
        }
        
        Map<String, Object> map = new HashMap<>();
        Student s = sl.getStudent();
        Teacher t = sl.getTeacher();
        ComDataUtil di = ComDataUtil.getInstance();
        
        map.put("studentLeaveId", sl.getStudentLeaveId());
        map.put("studentNum", s.getPerson().getNum());
        map.put("studentName", s.getPerson().getName());
        map.put("studentId", s.getPersonId());
        map.put("teacherName", t.getPerson().getName());
        map.put("teacherNum", t.getPerson().getNum());
        map.put("state", sl.getState());
        map.put("stateName", di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
        map.put("reason", sl.getReason());
        map.put("leaveDate", sl.getLeaveDate());
        map.put("adminComment", sl.getAdminComment());
        map.put("teacherComment", sl.getTeacherComment());
        map.put("applyTime", sl.getApplyTime());
        map.put("teacherTime", sl.getTeacherTime());
        map.put("adminTime", sl.getAdminTime());
        
        return CommonMethod.getReturnData(map);
    }

    public DataResponse updateLeaveApplication(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String userName = CommonMethod.getUsername();
        Integer teacherId = dataRequest.getInteger("teacherId");
        String leaveDate = dataRequest.getString("leaveDate");
        String reason = dataRequest.getString("reason");
        
        if(studentLeaveId == null || studentLeaveId <= 0) {
            return CommonMethod.getReturnMessageError("请假申请ID不能为空");
        }
        
        Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
        if(!op.isPresent()) {
            return CommonMethod.getReturnMessageError("请假申请不存在");
        }
        
        StudentLeave sl = op.get();
        // 验证是否为当前学生的请假申请
        if(!sl.getStudent().getPerson().getNum().equals(userName)) {
            return CommonMethod.getReturnMessageError("无权限修改此请假申请");
        }
        
        // 只有待审核状态(0)才能修改
        if(sl.getState() != 0) {
            return CommonMethod.getReturnMessageError("只有待审核状态的请假申请才能修改");
        }
        
        // 更新请假信息
        if(teacherId != null && teacherId > 0) {
            Optional<Teacher> teacherOp = teacherRepository.findById(teacherId);
            if(teacherOp.isPresent()) {
                sl.setTeacher(teacherOp.get());
            }
        }
        
        if(leaveDate != null && !leaveDate.trim().isEmpty()) {
            sl.setLeaveDate(leaveDate);
        }
        
        if(reason != null && !reason.trim().isEmpty()) {
            sl.setReason(reason);
        }
        
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse cancelLeaveApplication(DataRequest dataRequest) {
        logger.info("开始处理撤销请假申请请求");
        try {
            Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
            String userName = CommonMethod.getUsername();
            
            logger.info("撤销请求参数 - studentLeaveId: {}, userName: {}", studentLeaveId, userName);
            
            // 验证参数
            if(studentLeaveId == null || studentLeaveId <= 0) {
                logger.warn("请假申请ID为空或无效: {}", studentLeaveId);
                return CommonMethod.getReturnMessageError("请假申请ID不能为空");
            }
            
            if(userName == null || userName.trim().isEmpty()) {
                logger.error("无法获取当前用户信息");
                return CommonMethod.getReturnMessageError("无法获取当前用户信息，请重新登录");
            }
            
            // 查找请假申请
            Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
            if(!op.isPresent()) {
                logger.warn("未找到ID为{}的请假申请", studentLeaveId);
                return CommonMethod.getReturnMessageError("请假申请不存在");
            }
            
            StudentLeave sl = op.get();
            logger.info("找到请假申请 - ID: {}, 状态: {}, 申请人: {}", 
                       sl.getStudentLeaveId(), sl.getState(), sl.getStudent().getPerson().getNum());
            
            // 验证是否为当前学生的请假申请
            String applicantNum = sl.getStudent().getPerson().getNum();
            if(!applicantNum.equals(userName)) {
                logger.warn("权限验证失败 - 申请人: {}, 当前用户: {}", applicantNum, userName);
                return CommonMethod.getReturnMessageError("无权限撤销此请假申请");
            }
            
            // 只有待审核状态(0)才能撤销
            if(sl.getState() != 0) {
                logger.warn("请假申请状态不允许撤销 - 当前状态: {}", sl.getState());
                String stateName = getStateName(sl.getState());
                return CommonMethod.getReturnMessageError("只有待审核状态的请假申请才能撤销，当前状态：" + stateName);
            }
            
            // 删除请假申请
            logger.info("准备删除请假申请 - ID: {}", sl.getStudentLeaveId());
            studentLeaveRepository.delete(sl);
            logger.info("请假申请撤销成功 - ID: {}", sl.getStudentLeaveId());
            
            return CommonMethod.getReturnMessageOK("请假申请撤销成功");
            
        } catch (Exception e) {
            logger.error("撤销请假申请时发生错误", e);
            return CommonMethod.getReturnMessageError("撤销请假申请失败: " + e.getMessage());
        }
    }
    
    private String getStateName(Integer state) {
        if (state == null) return "未知";
        switch (state) {
            case 0: return "待审核";
            case 1: return "教师已通过";
            case 2: return "教师已拒绝";
            case 3: return "管理员已通过";
            case 4: return "管理员已拒绝";
            default: return "未知状态";
        }
    }

    public DataResponse getMyLeaveStatistics(DataRequest dataRequest) {
        String userName = CommonMethod.getUsername();
        
        // 获取当前学生的所有请假记录
        List<StudentLeave> allLeaves = studentLeaveRepository.getStudentLeaveList(-1, "", userName, "");
        
        Map<String, Object> statistics = new HashMap<>();
        int totalCount = allLeaves.size();
        int pendingCount = 0;  // 待审核
        int approvedCount = 0; // 已通过
        int rejectedCount = 0; // 已拒绝
        
        for (StudentLeave leave : allLeaves) {
            switch (leave.getState()) {
                case 0:
                    pendingCount++;
                    break;
                case 1:
                    approvedCount++;
                    break;
                case 2:
                    rejectedCount++;
                    break;
                case 3:
                    approvedCount++; // 管理员已通过
                    break;
                case 4:
                    rejectedCount++; // 管理员已拒绝
                    break;
            }
        }
        
        statistics.put("totalCount", totalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("approvedCount", approvedCount);
        statistics.put("rejectedCount", rejectedCount);
        
        // 按月份统计
        Map<String, Integer> monthlyStats = new HashMap<>();
        for (StudentLeave leave : allLeaves) {
            String leaveDate = leave.getLeaveDate();
            if (leaveDate != null && leaveDate.length() >= 7) {
                String month = leaveDate.substring(0, 7); // YYYY-MM
                monthlyStats.put(month, monthlyStats.getOrDefault(month, 0) + 1);
            }
        }
        statistics.put("monthlyStats", monthlyStats);
        
        return CommonMethod.getReturnData(statistics);
    }

    public DataResponse batchCancelLeaveApplications(DataRequest dataRequest) {
        List<?> studentLeaveIds = dataRequest.getList("studentLeaveIds");
        String userName = CommonMethod.getUsername();
        
        if(studentLeaveIds == null || studentLeaveIds.isEmpty()) {
            return CommonMethod.getReturnMessageError("请选择要撤销的请假申请");
        }
        
        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        for (Object idObj : studentLeaveIds) {
            try {
                Integer studentLeaveId = null;
                if (idObj instanceof Integer) {
                    studentLeaveId = (Integer) idObj;
                } else if (idObj instanceof String) {
                    studentLeaveId = Integer.parseInt((String) idObj);
                } else if (idObj instanceof Double) {
                    studentLeaveId = ((Double) idObj).intValue();
                }
                
                if (studentLeaveId == null || studentLeaveId <= 0) {
                    failCount++;
                    errorMessages.add("无效的请假申请ID: " + idObj);
                    continue;
                }
                
                Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
                if (!op.isPresent()) {
                    failCount++;
                    errorMessages.add("请假申请不存在: " + studentLeaveId);
                    continue;
                }
                
                StudentLeave sl = op.get();
                // 验证是否为当前学生的请假申请
                if (!sl.getStudent().getPerson().getNum().equals(userName)) {
                    failCount++;
                    errorMessages.add("无权限撤销请假申请: " + studentLeaveId);
                    continue;
                }
                
                // 只有待审核状态(0)才能撤销
                if (sl.getState() != 0) {
                    failCount++;
                    errorMessages.add("只有待审核状态的请假申请才能撤销: " + studentLeaveId);
                    continue;
                }
                
                // 删除请假申请
                studentLeaveRepository.delete(sl);
                successCount++;
                
            } catch (Exception e) {
                failCount++;
                errorMessages.add("处理请假申请ID " + idObj + " 时出错: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errorMessages", errorMessages);
        
        if (failCount == 0) {
            return CommonMethod.getReturnData(result);
        } else if (successCount == 0) {
            return CommonMethod.getReturnMessageError("批量撤销失败: " + String.join("; ", errorMessages));
        } else {
            return CommonMethod.getReturnData(result);
        }
    }
}