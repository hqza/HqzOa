package com.athqz.wechat.service.impl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.athqz.model.process.Process;
import com.athqz.model.process.ProcessTemplate;
import com.athqz.model.system.SysUser;
import com.athqz.process.service.OaProcessService;
import com.athqz.process.service.OaProcessTemplateService;
import com.athqz.security.custom.LoginUserInfoHelper;
import com.athqz.auth.service.SysUserService;
import com.athqz.wechat.service.MessageService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private WxMpService wxMpService;



    @Value("${wechaturlhttp}")      //前段接口地址
    String wechaturlhttp;

    @Value("${pushProcessedMessageId}")       //审批后推送提交审批人员
    String pushProcessedMessageId;

    @Value("${pushPendingMessageId}")       //推送待审批人员
    String pushPendingMessageId;

    @Value("${developerOpenId}")        //方便测试，给默认值（开发者本人的openId）
    String developerOpenId;


    @Resource
    private OaProcessService processService;



    @Resource
    private OaProcessTemplateService processTemplateService;

    @Resource
    private SysUserService sysUserService;

    @SneakyThrows
    @Override
    public void pushPendingMessage(Long processId, Long userId, String taskId) {
        //查询流程信息
        Process process = processService.getById(processId);
        //查询审批模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        //根据userId查询要推送人信息
        SysUser sysUser = sysUserService.getById(userId);
        //提交人信息
        SysUser submitSysUser = sysUserService.getById(process.getUserId());
        String openid = sysUser.getOpenId();
        //方便测试，给默认值（开发者本人的openId）
        if(StringUtils.isEmpty(openid)) {
            openid = developerOpenId;
        }
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                .toUser(openid)//要推送的用户openid
                //创建的模板id
                .templateId(pushPendingMessageId)//模板id
                .url(wechaturlhttp+"/#/show/"+processId+"/"+taskId)//点击模板消息要访问的网址
                .build();
        JSONObject jsonObject = JSON.parseObject(process.getFormValues());
        JSONObject formShowData = jsonObject.getJSONObject("formShowData");
        StringBuffer content = new StringBuffer();
        for (Map.Entry entry : formShowData.entrySet()) {
            content.append(entry.getKey()).append("：").append(entry.getValue()).append("\n ");
        }
        //设置模板参数
        templateMessage.addData(new WxMpTemplateData("first", submitSysUser.getName()+"提交了"+processTemplate.getName()+"审批申请，请注意查看。", "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword1", process.getProcessCode(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword2", new DateTime(process.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss"), "#272727"));
        templateMessage.addData(new WxMpTemplateData("content", content.toString(), "#272727"));
        String msg = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
        log.info("推送消息返回：{}", msg);
    }

    @SneakyThrows
    @Override
    public void pushProcessedMessage(Long processId, Long userId, Integer status) {
        Process process = processService.getById(processId);
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        SysUser sysUser = sysUserService.getById(userId);
        SysUser currentSysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        String openid = sysUser.getOpenId();
        if(StringUtils.isEmpty(openid)) {
            openid = developerOpenId;
        }
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                .toUser(openid)//要推送的用户openid
                .templateId(pushProcessedMessageId)//模板id
                .url(wechaturlhttp+"/#/show/"+processId+"/0")//点击模板消息要访问的网址
                .build();
        JSONObject jsonObject = JSON.parseObject(process.getFormValues());
        JSONObject formShowData = jsonObject.getJSONObject("formShowData");
        StringBuffer content = new StringBuffer();
        for (Map.Entry entry : formShowData.entrySet()) {
            content.append(entry.getKey()).append("：").append(entry.getValue()).append("\n ");
        }
        templateMessage.addData(new WxMpTemplateData("first", "你发起的"+processTemplate.getName()+"审批申请已经被处理了，请注意查看。", "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword1", process.getProcessCode(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword2", new DateTime(process.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss"), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword3", currentSysUser.getName(), "#272727"));
        templateMessage.addData(new WxMpTemplateData("keyword4", status == 1 ? "审批通过" : "审批拒绝", status == 1 ? "#009966" : "#FF0033"));
        templateMessage.addData(new WxMpTemplateData("content", content.toString(), "#272727"));
        String msg = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
        log.info("推送消息返回：{}", msg);
    }

}