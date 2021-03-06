package teammates.test.cases.ui.browsertests;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.test.driver.BackDoor;
import teammates.test.pageobjects.AppPage;
import teammates.test.pageobjects.Browser;
import teammates.test.pageobjects.BrowserPool;
import teammates.test.pageobjects.FeedbackQuestionSubmitPage;

public class StudentFeedbackQuestionSubmitPageUiTest extends BaseUiTestCase {
    private static DataBundle testData;
    private static Browser browser;
    private static FeedbackQuestionSubmitPage submitPage;
    private static FeedbackQuestionAttributes fq;
    private static Date fsOriginalEndTime;

    @BeforeClass
    public static void classSetup() {
        printTestClassHeader();
        testData = loadDataBundle("/StudentFeedbackQuestionSubmitPageUiTest.json");
        removeAndRestoreTestDataOnServer(testData);
        fsOriginalEndTime = testData.feedbackSessions.get("Open Session").getEndTime();
        
        browser = BrowserPool.getBrowser();
    }

    @Test
    public void testAll() throws Exception {
        testContent();
        testSubmitAction();
        // no links to test
    }

    private void testContent() throws Exception {

        ______TS("unreg student");
        
        logout(browser);
        
        FeedbackQuestionAttributes fqOpen = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Open Session", 1);
        FeedbackQuestionAttributes fqClosed = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Closed Session", 1);
        
        // Open session
        StudentAttributes unregStudent = testData.students.get("Unregistered");
        submitPage = goToStudentFeedbackQuestionSubmitPage(unregStudent, "Open Session", fqOpen.getId());

        // This is the full HTML verification for Unregistered Student Feedback Question Submit Page, the rest can all be verifyMainHtml
        submitPage.verifyHtml("/unregisteredStudentFeedbackQuestionSubmitPageOpen.html");
        
        // closed session
        submitPage = goToStudentFeedbackQuestionSubmitPage(unregStudent, "Closed Session", fqClosed.getId());
        submitPage.verifyHtmlMainContent("/unregisteredStudentFeedbackQuestionSubmitPageClosed.html");
        
        ______TS("Awaiting session");

        fq = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Awaiting Session", 1);
        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Awaiting Session", fq.getId());

        // This is the full HTML verification for Registered Student Feedback Question Submit Page, the rest can all be verifyMainHtml
        submitPage.verifyHtml("/studentFeedbackQuestionSubmitPageAwaiting.html");
        
        ______TS("Open session");

        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fqOpen.getId());
        submitPage.verifyHtmlMainContent("/studentFeedbackQuestionSubmitPageOpen.html");

        ______TS("Grace period session");

        FeedbackSessionAttributes fs = BackDoor.getFeedbackSession("SFQSubmitUiT.CS2104", "Open Session");
        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fqOpen.getId());

        Calendar endDate = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        fs.setTimeZone(0);
        endDate.add(Calendar.MINUTE, -1);
        fs.setEndTime(endDate.getTime());
        fs.setGracePeriod(10);
        BackDoor.editFeedbackSession(fs);

        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fqOpen.getId());

        assertFalse(submitPage.getSubmitButton().isEnabled());

        ______TS("Closed session");

        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Closed Session", fqClosed.getId());
        submitPage.verifyHtmlMainContent("/studentFeedbackQuestionSubmitPageClosed.html");
    }

    private void testSubmitAction() throws Exception {
        FeedbackSessionAttributes fs = BackDoor.getFeedbackSession("SFQSubmitUiT.CS2104", "Open Session");
        fs.setEndTime(fsOriginalEndTime);
        BackDoor.editFeedbackSession(fs);
        
        ______TS("create new responses");

        fq = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Open Session", 1);
        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fq.getId());

        submitPage.fillResponseTextBox(1, 0, "Test Self Feedback");
        assertNull(BackDoor.getFeedbackResponse(fq.getId(),
                                                "SFQSubmitUiT.alice.b@gmail.tmt",
                                                "SFQSubmitUiT.alice.b@gmail.tmt"));

        submitPage.clickSubmitButton();

        assertEquals(Const.StatusMessages.FEEDBACK_RESPONSES_SAVED, submitPage.getStatus());

        assertNotNull(BackDoor.getFeedbackResponse(fq.getId(),
                                                   "SFQSubmitUiT.alice.b@gmail.tmt",
                                                   "SFQSubmitUiT.alice.b@gmail.tmt"));
        
        assertEquals("Test Self Feedback",
                     BackDoor.getFeedbackResponse(fq.getId(),
                                                  "SFQSubmitUiT.alice.b@gmail.tmt",
                                                  "SFQSubmitUiT.alice.b@gmail.tmt").getResponseDetails().getAnswerString());

        ______TS("edit existing response");

        String editedResponse = "Edited self feedback.";
        submitPage.fillResponseTextBox(1, 0, editedResponse);
        submitPage.clickSubmitButton();

        assertEquals(Const.StatusMessages.FEEDBACK_RESPONSES_SAVED, submitPage.getStatus());
        assertNotNull(BackDoor.getFeedbackResponse(fq.getId(),
                                                   "SFQSubmitUiT.alice.b@gmail.tmt",
                                                   "SFQSubmitUiT.alice.b@gmail.tmt"));
        
        assertEquals(editedResponse,
                     BackDoor.getFeedbackResponse(fq.getId(),
                                                  "SFQSubmitUiT.alice.b@gmail.tmt",
                                                  "SFQSubmitUiT.alice.b@gmail.tmt").getResponseDetails().getAnswerString());
        
        submitPage.verifyHtmlMainContent("/studentFeedbackQuestionSubmitPageFilled.html");

        ______TS("Grace period session,successful submission within grace period");

        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fq.getId());

        Calendar endDate = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));

        fs.setTimeZone(0);
        endDate.add(Calendar.MINUTE, -1);
        fs.setEndTime(endDate.getTime());
        fs.setGracePeriod(10);
        BackDoor.editFeedbackSession(fs);

        submitPage.fillResponseTextBox(1, 0, "this is a response edited during grace period");
        submitPage.clickSubmitButton();

        assertTrue(submitPage.getStatus().contains(Const.StatusMessages.FEEDBACK_RESPONSES_SAVED));

        // test if the button is disabled after the response has been submitted
        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fq.getId());
        assertFalse(submitPage.getSubmitButton().isEnabled());

        // test the response submitted during the grace period
        fs = BackDoor.getFeedbackSession("SFQSubmitUiT.CS2104", "Open Session");
        fq = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Open Session", 1);
        
        assertEquals("this is a response edited during grace period",
                     BackDoor.getFeedbackResponse(fq.getId(),
                                                  "SFQSubmitUiT.alice.b@gmail.tmt",
                                                  "SFQSubmitUiT.alice.b@gmail.tmt").getResponseDetails().getAnswerString());
        
        assertEquals("this is a response edited during grace period", submitPage.getTextArea(1, 0).getText());

        logout(browser);
        fs = BackDoor.getFeedbackSession("SFQSubmitUiT.CS2104", "Open Session");
        fs.setEndTime(fsOriginalEndTime);
        BackDoor.editFeedbackSession(fs);

        ______TS("Grace period session,submission failure after grace period");

        fq = BackDoor.getFeedbackQuestion("SFQSubmitUiT.CS2104", "Open Session", 1);
        fs = BackDoor.getFeedbackSession("SFQSubmitUiT.CS2104", "Open Session");
        
        endDate = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));

        submitPage = loginToStudentFeedbackQuestionSubmitPage("Alice", "Open Session", fq.getId());

        fs.setTimeZone(0);
        endDate.add(Calendar.MINUTE, -20);
        fs.setEndTime(endDate.getTime());
        fs.setGracePeriod(10);
        BackDoor.editFeedbackSession(fs);

        submitPage.fillResponseTextBox(1, 0, "this is a response edited during grace period,but submitted after grace period");
        submitPage.clickSubmitButton();
        submitPage.verifyHtmlMainContent("/studentFeedbackQuestionSubmitPageDeadLineExceeded.html");
        
    }

    private FeedbackQuestionSubmitPage loginToStudentFeedbackQuestionSubmitPage(
            String studentName, String fsName, String questionId) {
        AppUrl editUrl = createUrl(Const.ActionURIs.STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_PAGE)
                .withUserId(testData.students.get(studentName).googleId)
                .withCourseId(testData.feedbackSessions.get(fsName).getCourseId())
                .withSessionName(testData.feedbackSessions.get(fsName).getFeedbackSessionName())
                .withParam(Const.ParamsNames.FEEDBACK_QUESTION_ID, questionId);
        
        return loginAdminToPage(browser, editUrl, FeedbackQuestionSubmitPage.class);
    }

    private FeedbackQuestionSubmitPage goToStudentFeedbackQuestionSubmitPage(
            StudentAttributes s, String fsName, String questionId) {
        AppUrl editUrl = createUrl(Const.ActionURIs.STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_PAGE)
                .withRegistrationKey(BackDoor.getKeyForStudent(s.course, s.email))
                .withStudentEmail(s.email)
                .withCourseId(s.course)
                .withSessionName(testData.feedbackSessions.get(fsName).getFeedbackSessionName())
                .withParam(Const.ParamsNames.FEEDBACK_QUESTION_ID, questionId);
        
        return AppPage.getNewPageInstance(browser, editUrl, FeedbackQuestionSubmitPage.class);
    }
    
    @AfterClass
    public static void classTearDown() {
        BrowserPool.release(browser);
    }
}
