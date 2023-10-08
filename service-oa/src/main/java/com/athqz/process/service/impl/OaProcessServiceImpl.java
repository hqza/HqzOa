package com.athqz.process.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.athqz.auth.service.SysUserService;
import com.athqz.common.result.Result;
import com.athqz.model.process.Process;
import com.athqz.model.process.ProcessRecord;
import com.athqz.model.process.ProcessTemplate;
import com.athqz.model.system.SysUser;
import com.athqz.process.mapper.OaProcessMapper;
import com.athqz.process.service.OaProcessRecordService;
import com.athqz.process.service.OaProcessService;
import com.athqz.process.service.OaProcessTemplateService;
import com.athqz.security.custom.LoginUserInfoHelper;
import com.athqz.vo.process.ApprovalVo;
import com.athqz.vo.process.ProcessFormVo;
import com.athqz.vo.process.ProcessQueryVo;
import com.athqz.vo.process.ProcessVo;
import com.athqz.wechat.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author plus
 * @since 2023-10-06
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OaProcessServiceImpl extends ServiceImpl<OaProcessMapper, Process> implements OaProcessService {


    @Autowired
    private MessageService messageService;

    @Autowired
    private OaProcessTemplateService processTemplateService;
    @Autowired
    private OaProcessMapper processMapper;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private OaProcessRecordService processRecordService;

    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        return page;
    }

    @Override
    public void deployByZip(String deployPath) {
        System.out.println("这是打印");
        // 定义zip输入流
        InputStream inputStream = this
                .getClass()
                .getClassLoader()
                .getResourceAsStream(deployPath);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // 流程部署
        Deployment deployment = repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .deploy();
        System.out.println("开始"+deployment.getId());
        System.out.println("审批名称"+deployment.getName());
    }

    @Override
    public void startUp(ProcessFormVo processFormVo) {
        //1根据当前用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());

        //2根据审批模板id把模板信息查询
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());

        //3保存提交的审批信息到业务表，oa_process
        Process process=new Process();
            //属性相同进行复制，不同属性不行
        BeanUtils.copyProperties(processFormVo,process);

        String workNo = System.currentTimeMillis() + "";
        process.setProcessCode(workNo);
        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName() + "申请");
        process.setStatus(1);
        processMapper.insert(process);
        //4启动流程实例RuntimeService

            //4.1流程定义的key
        String processDefinitionKey = processTemplate.getProcessDefinitionKey();
            //4.2流程业务key processId
        String businessKey = String.valueOf(process.getId());
            //4.3流程参数form表单json数据，转换成map集合
        String formValues = processFormVo.getFormValues();
        JSONObject jsonObject = JSON.parseObject(formValues);
        JSONObject formData = jsonObject.getJSONObject("formData");
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", map);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);

        //5查询下一个审批人是谁
        String processInstanceId = processInstance.getId();
        process.setProcessInstanceId(processInstanceId);
        List<Task> taskList = this.getCurrentTaskList(processInstanceId);
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assigneeList = new ArrayList<>();
            for (Task task : taskList) {
                SysUser user = sysUserService.getByUsername(task.getAssignee());
                assigneeList.add(user.getName());
                //6推送一个消息
                messageService.pushPendingMessage(process.getId(), sysUser.getId(), task.getId());
            }
            process.setDescription("等待" + StringUtils.join(assigneeList.toArray(), ",") + "审批");
        }
            //7业务和流程进行关联，更新oa_process数据
        processMapper.updateById(process);
        processRecordService.record(process.getId(),1,"发起申请");
    }

    /**
     * 获取当前任务列表
     * @param processInstanceId
     * @return
     */
    private List<Task> getCurrentTaskList(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        return tasks;
    }

    @Override
    public IPage<ProcessVo> findPending(Page<Process> pageParam) {
        // 根据当前人的ID查询用户名称
        TaskQuery query = taskService.createTaskQuery().taskAssignee(LoginUserInfoHelper.getUsername()).orderByTaskCreateTime().desc();
        //调用方法分页条件查询，返回list集合，带班任务集合
                                    //参数1：开始位置-----参数2：每页记录数
        int begin=(int) ((pageParam.getCurrent() - 1) * pageParam.getSize());
        int size=(int) pageParam.getSize();
        List<Task> list = query.listPage(begin,size);
        long totalCount = query.count();
        //List<Task>变成List<ProcessVo>
        List<ProcessVo> processList = new ArrayList<>();
        // 根据流程的业务ID查询实体并关联
        for (Task item : list) {
            //获取实例ID
            String processInstanceId = item.getProcessInstanceId();
            //根据id获取实例对象
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                continue;
            }
            // 业务key获取Process对象
            String businessKey = processInstance.getBusinessKey();
            if (businessKey == null) {
                continue;
            }
            Process process = this.getById(Long.parseLong(businessKey));

            //Process对象，复制ProcessVo对象
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId(item.getId());
            //放到最终List集合processVoList
            processList.add(processVo);
        }
        //封装返回Ipage对象
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    @Override
    public Map<String, Object> show(Long id) {
        //根据流程id获取流程信息Process
        Process process = this.getById(id);
        //根据流程id获取流程记录信息
        List<ProcessRecord> processRecordList = processRecordService.list(new LambdaQueryWrapper<ProcessRecord>().eq(ProcessRecord::getProcessId, id));
        //根据模版id查询模版信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());

        //查询数据封装到map
        Map<String, Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        //计算当前用户是否可以审批，能够查看详情的用户不是都能审批，审批后也不能重复审批
        boolean isApprove = false;
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            for(Task task : taskList) {
                if(task.getAssignee().equals(LoginUserInfoHelper.getUsername())) {
                    isApprove = true;
                }
            }
        }
        map.put("isApprove", isApprove);
        return map;
    }

    public void approve(ApprovalVo approvalVo) {
        //从approvalvo获取任务id,根据任务id获取流程变量
        Map<String, Object> variables1 = taskService.getVariables(approvalVo.getTaskId());
        for (Map.Entry<String, Object> entry : variables1.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }
        //判断审批状态值【1通过】【!1驳回】
        String taskId = approvalVo.getTaskId();
        if (approvalVo.getStatus() == 1) {
            //已通过
            Map<String, Object> variables = new HashMap<String, Object>();
            taskService.complete(taskId, variables);
        } else {
            //驳回
            this.endTask(taskId);
        }
        //记录审批相关过程信息oa_process_record
        String description = approvalVo.getStatus().intValue() == 1 ? "已通过" : "驳回";
        processRecordService.record(approvalVo.getProcessId(), approvalVo.getStatus(), description);

        //计算下一个审批人
        Process process = this.getById(approvalVo.getProcessId());
        //查询任务
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assigneeList = new ArrayList<>();
            for(Task task : taskList) {
                SysUser sysUser = sysUserService.getByUsername(task.getAssignee());
                assigneeList.add(sysUser.getName());

                //推送消息给下一个审批人【微信公众号】
            }
            process.setDescription("等待" + StringUtils.join(assigneeList.toArray(), ",") + "审批");
            process.setStatus(1);
        } else {
            if(approvalVo.getStatus().intValue() == 1) {
                process.setDescription("审批完成（同意）");
                process.setStatus(2);
            } else {
                process.setDescription("审批完成（拒绝）");
                process.setStatus(-1);
            }
        }
        //推送消息给申请人
        messageService.pushProcessedMessage(process.getId(), process.getUserId(), approvalVo.getStatus());
        this.updateById(process);
    }

    @Autowired
    private HistoryService historyService;

    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        // 根据当前人的ID查询
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskAssignee(LoginUserInfoHelper.getUsername()).finished().orderByTaskCreateTime().desc();
        //调用方法条件分页查询，返回list集合
        List<HistoricTaskInstance> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        long totalCount = query.count();
        //遍历返回list集合，封装lsit<proessVo>
        List<ProcessVo> processList = new ArrayList<>();
        for (HistoricTaskInstance item : list) {
            //流程实例id
            String processInstanceId = item.getProcessInstanceId();
            //流程实例id产讯获取process信息
            Process process = this.getOne(new LambdaQueryWrapper<Process>().eq(Process::getProcessInstanceId, processInstanceId));
            ProcessVo processVo = new ProcessVo();
            if (process==null)
                continue;
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId("0");
            //放到list
            processList.add(processVo);
        }
        //IPge封装分页查询所有数据返回
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    @Override
    public IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        for (ProcessVo item : page.getRecords()) {
            item.setTaskId("0");
        }
        return page;
    }

    private void endTask(String taskId) {
        //  当前任务对象
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //获取流程定义模型BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        //获取结束流向节点
        List endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        // 并行任务可能为null
        if(CollectionUtils.isEmpty(endEventList)) {
            return;
        }
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);
        //当前流向节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        //清楚当前流动方向
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endFlowNode);
        //当前节点新方向
        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());
    }
}
