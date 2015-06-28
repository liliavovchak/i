package org.activiti.rest.controller;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.redis.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.wf.dp.dniprorada.constant.HistoryEventType;
import org.wf.dp.dniprorada.dao.*;
import org.wf.dp.dniprorada.model.*;
import org.wf.dp.dniprorada.model.document.DocumentNotFoundException;
import org.wf.dp.dniprorada.model.document.HandlerFactory;
import org.wf.dp.dniprorada.util.Util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

@Controller
@RequestMapping(value = "/services")
public class ActivitiRestDocumentController {

    private static final Logger log = LoggerFactory.getLogger(ActivitiRestDocumentController.class);
    
    @Autowired
    private DocumentDao documentDao;

    @Autowired
    private SubjectDao subjectDao;
    
    @Autowired
    private SubjectOrganDao subjectOrganDao;

    @Autowired
    private HistoryEventDao historyEventDao;

    @Autowired
    private DocumentContentTypeDao documentContentTypeDao;

    @Autowired
    private HandlerFactory handlerFactory;

    @RequestMapping(value = "/getDocument", method = RequestMethod.GET)
    public
    @ResponseBody
    Document getDocument(@RequestParam(value = "nID") Long id,
            @RequestParam(value = "nID_Subject") long nID_Subject) throws ActivitiRestException{
        Document document = documentDao.getDocument(id);
        if(nID_Subject != document.getSubject().getnID()){  
            throw new ActivitiRestException("401", "You don't have access! Your nID = " + nID_Subject + " Document's Subject's nID = " + document.getSubject().getnID());
        } else{
            return  document;
        }
    }



    /**
     * @param accessCode    - строковой код доступа к документу
     * @param organID	    - номер-ИД субьекта-органа оператора документа
     * @param docTypeID	    - номер-ИД типа документа
     * @param password	    - строка-пароль (опционально)
     * */
    @RequestMapping(value 	= "/getDocumentAccessByHandler",
                    method 	= RequestMethod.GET,
                    headers = { "Accept=application/json" })
    public @ResponseBody
    Document getDocumentAccessByHandler(
            @RequestParam(value = "sCode_DocumentAccess") 				String 	accessCode,
            @RequestParam(value = "nID_DocumentOperator_SubjectOrgan") 	Long 	organID,
            @RequestParam(value = "nID_DocumentType", required = false) Integer	docTypeID,
            @RequestParam(value = "sPass", required = false)		    String 	password,
            HttpServletResponse resp) {

        DocumentAccess docAccess = handlerFactory
            .buildHandlerFor(documentDao.getOperator(organID))
            .setAccessCode(accessCode)
            .setPassword(password)
            .getAccess();

        Document doc = documentDao.getDocument( docAccess.getID() );

        if (docTypeID != null && !doc.getDocumentType().getId().equals(docTypeID))
            throw new DocumentNotFoundException("Access code " + accessCode + ", type " + docTypeID);

        return doc;
    }


    @RequestMapping(value 	= "/getDocumentOperators",
                    method 	= RequestMethod.GET,
                    headers = { "Accept=application/json" })
    public @ResponseBody List<DocumentOperator_SubjectOrgan> getDocumentOperators() {
        return documentDao.getAllOperators();
    }


    @RequestMapping(value = "/getHistoryEvent", method = RequestMethod.GET)
    public
    @ResponseBody
    HistoryEvent getHistoryEvent(@RequestParam(value = "nID") Long id) {
        return historyEventDao.getHistoryEvent(id);
    }

    @RequestMapping(value = "/getDocumentContent", method = RequestMethod.GET)
    public
    @ResponseBody
    String getDocumentContent(@RequestParam(value = "nID") Long id, 
            @RequestParam(value = "nID_Subject") long nID_Subject) throws ActivitiRestException {
        Document document = documentDao.getDocument(id);
        if(nID_Subject != document.getSubject().getnID()){
            throw new ActivitiRestException("401", "You don't have access!");
        } else{
            return Util.contentByteToString(documentDao.getDocumentContent(document.getСontentKey())); // ????
        }
    }

    @RequestMapping(value = "/getHistoryEvents", method = RequestMethod.GET)
    public
    @ResponseBody
    List<HistoryEvent> getHistoryEvents(
            @RequestParam(value = "nID_Subject") long nID_Subject) {
        return historyEventDao.getHistoryEvents(nID_Subject);
    }

    @RequestMapping(value = "/setHistoryEvent", method = RequestMethod.POST)
    public
    @ResponseBody
    Long setHistoryEvent(
            @RequestParam(value = "nID_Subject", required = false) long nID_Subject,
            @RequestParam(value = "nID_HistoryEventType", required = false) Long nID_HistoryEventType,
            @RequestParam(value = "sEventName", required = false) String sEventName_Custom,
            @RequestParam(value = "sMessage") String sMessage,

            HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {


        return historyEventDao.setHistoryEvent(
                nID_Subject,
                nID_HistoryEventType,
                sEventName_Custom,
                sMessage);

    }


    @RequestMapping(value = "/getDocumentFile", method = RequestMethod.GET)
    public
    @ResponseBody
    byte[] getDocumentFile(@RequestParam(value = "nID") Long id,
            @RequestParam(value = "nID_Subject") long nID_Subject,
                           HttpServletRequest request, HttpServletResponse httpResponse) 
                           throws ActivitiRestException{
        Document document = documentDao.getDocument(id);
        if(nID_Subject != document.getSubject().getnID()){
            throw new ActivitiRestException("401", "You don't have access!");
        } 
        byte[] content = documentDao.getDocumentContent(document
                .getСontentKey());
        //byte[] content = "".getBytes();
        
        httpResponse.setHeader("Content-disposition", "attachment; filename="
                + document.getFile());
        //httpResponse.setHeader("Content-Type", document.getDocumentContentType()
        //		.getName() + ";charset=UTF-8");
        httpResponse.setHeader("Content-Type", document.getContentType() + ";charset=UTF-8");
        httpResponse.setContentLength(content.length);
        return content;
    }

    @RequestMapping(value = "/getDocuments", method = RequestMethod.GET)
    public
    @ResponseBody
    List<Document> getDocuments(
            @RequestParam(value = "nID_Subject") long nID_Subject) {
        return documentDao.getDocuments(nID_Subject);
    }

    @RequestMapping(value = "/setDocument", method = RequestMethod.GET)
    public
    @ResponseBody
    Long setDocument(
            @RequestParam(value = "nID_Subject", required = false) long nID_Subject,
            @RequestParam(value = "sID_Subject_Upload") String sID_Subject_Upload,
            @RequestParam(value = "sSubjectName_Upload") String sSubjectName_Upload,
            @RequestParam(value = "sName") String sName,
            //@RequestParam(value = "sFile", required = false) String fileName,
            @RequestParam(value = "nID_DocumentType") Integer nID_DocumentType,
            //@RequestParam(value = "nID_DocumentContentType", required = false) Integer nID_DocumentContentType,
            @RequestParam(value = "sDocumentContentType", required = false) String documentContentTypeName,
            @RequestParam(value = "soDocumentContent") String sContent,
            //@RequestParam(value = "oFile", required = false) MultipartFile oFile,
            //@RequestBody byte[] content,
            HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {

        //MultipartFile oFile = new MockMultipartFile("filename.txt", "fullfilename.txt", "text/plain", sContent.getBytes());
        String sFileName = "filename.txt";
        String sFileContentType = "text/plain";
        byte[] aoContent = sContent.getBytes();


        documentContentTypeName = request.getHeader("Content-Type") != null ? request.getHeader("filename") : documentContentTypeName;
        DocumentContentType documentContentType = null;
        if (documentContentTypeName != null) {
            documentContentType = documentContentTypeDao.getDocumentContentType(documentContentTypeName);
            if (documentContentType == null) {
                documentContentType = new DocumentContentType();
                documentContentType.setName(documentContentTypeName);
                documentContentType.setId(documentContentTypeDao.setDocumentContent(documentContentType));
            }
        } else {
            throw new ActivitiObjectNotFoundException(
                    "RequestParam 'nID_DocumentContentType' not found!", DocumentContentType.class);
        }
        
        Subject subject_Upload = syncSubject_Upload(sID_Subject_Upload);

        return documentDao.setDocument(
                nID_Subject,
                subject_Upload.getnID(),
                sID_Subject_Upload,
                sSubjectName_Upload,
                sName,
                nID_DocumentType,
                documentContentType.getId(),
                sFileName,
                sFileContentType,
                aoContent);

    }

    @RequestMapping(value = "/setDocumentFile", method = RequestMethod.POST)
    public
    @ResponseBody
    Long setDocumentFile(
            @RequestParam(value = "nID_Subject", required = false) long nID_Subject,
            @RequestParam(value = "sID_Subject_Upload") String sID_Subject_Upload,
            @RequestParam(value = "sSubjectName_Upload") String sSubjectName_Upload,
            @RequestParam(value = "sName") String sName,
            @RequestParam(value = "sFileExtension", required = false) String sFileExtension,
            //@RequestParam(value = "sFile", required = false) String fileName,
            @RequestParam(value = "nID_DocumentType") Integer nID_DocumentType,
            @RequestParam(value = "nID_DocumentContentType", required = false) Integer nID_DocumentContentType,
            @RequestParam(value = "oFile", required = true) MultipartFile oFile,
            //@RequestBody byte[] content,
            HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {

        //String sFileName = oFile.getName();
        //String sFileName = oFile.getOriginalFilename();
         //Content-Disposition:attachment; filename=passport.zip
        String sFileName = request.getHeader("filename");
        if(sFileName==null||"".equals(sFileName.trim())){
            //sFileName = oFile.getOriginalFilename()+".zip";
            String sOriginalFileName = oFile.getOriginalFilename();
            String sOriginalContentType = oFile.getContentType();
            log.info("sFileExtension="+sFileExtension);
            log.info("sOriginalFileName="+sOriginalFileName);
            log.info("sOriginalContentType="+sOriginalContentType);
            //for(String s : request.getHeaderNames()){
            Enumeration<String> a =  request.getHeaderNames();
            for(int n=0;a.hasMoreElements()&&n<100;n++){
                String s = a.nextElement();
                log.info("n="+n+", s="+s+", value="+request.getHeader(s));
            }
            String fileExp = RedisUtil.getFileExp(sOriginalFileName);
            fileExp = fileExp != null ? fileExp : ".zip.zip";
            //fileExp = fileExp.equalsIgnoreCase(sOriginalFileName) ? ".zip" : fileExp;
            fileExp = fileExp.equalsIgnoreCase(sOriginalFileName) ? sFileExtension : fileExp;
            fileExp = fileExp != null ? fileExp.toLowerCase() : ".zip";
            sFileName = sOriginalFileName + (fileExp.startsWith(".")?"":".") + fileExp;
            log.info("sFileName="+sFileName);
        }
        String sFileContentType = oFile.getContentType();
        byte[] aoContent = oFile.getBytes();

        Subject subject_Upload = syncSubject_Upload(sID_Subject_Upload);
        Long nID_Document = documentDao.setDocument(
                        nID_Subject,
                        subject_Upload.getnID(),
                        sID_Subject_Upload,
                        sSubjectName_Upload,
                        sName,
                        nID_DocumentType,
                        nID_DocumentContentType,
                        sFileName,
                        sFileContentType,
                        aoContent);
        createHistoryEvent(2L, nID_Subject, sSubjectName_Upload, sName, nID_Document);
        return nID_Document;
    }

    private Subject syncSubject_Upload(String sID_Subject_Upload){
    	Subject subject_Upload = subjectDao.getSubject(sID_Subject_Upload);
    	if(subject_Upload == null){
    		subject_Upload = subjectOrganDao.setSubjectOrgan(sID_Subject_Upload).getoSubject();
    	}
    	return subject_Upload;
    }

    private void createHistoryEvent(Long nID_HistoryEventType, Long nID_Subject,
                                    String sSubjectName_Upload,
                                    String sDocumentName, Long nID_Document) {
        String sDocumentType = "";
        try {
            Document oDocument = documentDao.getDocument(nID_Document);
            sDocumentType = oDocument.getDocumentType().getName();
        } catch (Throwable e) {
            log.error("can't get document info!", e);
        }
        try {
            HistoryEventType eventType = HistoryEventType.getById(nID_HistoryEventType);
            String eventMessage = eventType.getsTemplate();//"%Назва органу% завантажує %Тип документу% %Назва документу% у Ваш розділ Мої документи"
            eventMessage = eventMessage.replaceAll("%Назва органу%", sSubjectName_Upload)
                    .replaceAll("%Тип документу%", sDocumentType)
                    .replaceAll("%Назва документу%", sDocumentName);

            historyEventDao.setHistoryEvent(nID_Subject,
                    nID_HistoryEventType, eventMessage, eventMessage);
        } catch (IOException e) {
            log.error("error during creating HistoryEvent", e);
        } catch (Throwable e) {
            log.warn(e.getMessage());//???
        }
    }
}
