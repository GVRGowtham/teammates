package teammates.ui.controller;

import java.util.ArrayList;
import java.util.List;

import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.logic.api.Logic;

public class InstructorFeedbackEditCopyPageAction extends Action {

    @Override
    protected ActionResult execute() {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        Assumption.assertNotNull(courseId);

        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        Assumption.assertNotNull(feedbackSessionName);

        List<InstructorAttributes> instructors = logic.getInstructorsForGoogleId(account.googleId);
        Assumption.assertNotNull(instructors);
        
        List<CourseAttributes> allCourses = logic.getCoursesForInstructor(account.googleId);
        
        List<CourseAttributes> coursesToAddToData = new ArrayList<CourseAttributes>();
        
        // Only add courses to data if the course is not archived and instructor has sufficient permissions
        for (CourseAttributes course : allCourses) {
            InstructorAttributes instructor = logic.getInstructorForGoogleId(course.getId(), account.googleId);
            
            boolean isAllowedToMakeSession =
                    instructor.isAllowedForPrivilege(Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION);
            boolean isArchived = Logic.isCourseArchived(course.getId(), account.googleId);

            if (!isArchived && isAllowedToMakeSession) {
                coursesToAddToData.add(course);
            }
        }
        
        CourseAttributes.sortByCreatedDate(coursesToAddToData);
        
        InstructorFeedbackEditCopyPageData data =
                new InstructorFeedbackEditCopyPageData(account, coursesToAddToData, courseId, feedbackSessionName);
        
        return createShowPageResult(Const.ViewURIs.INSTRUCTOR_FEEDBACK_COPY_MODAL, data);
    }

}
