package com.example.timesheet.repository;

import com.example.timesheet.models.TimesheetSummary;
import com.example.timesheet.keys.TimesheetSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimesheetSummaryRepository extends JpaRepository<TimesheetSummary, TimesheetSummaryId>, JpaSpecificationExecutor<TimesheetSummary> {

    List<TimesheetSummary> findByIdEmployeeCode(String employeeCode);

    List<TimesheetSummary> findByIdEmployeeCodeAndIdTimesheetYearAndIdTimesheetMonth(
            String employeeCode, Integer year, Integer month);
    Optional<TimesheetSummary> findByIdEmployeeCodeAndIdWeekStart(String employeeCode, Date weekStart);
    List<TimesheetSummary> findByIdEmployeeCodeInAndIdWeekStartAndIdTimesheetYearAndIdTimesheetMonth(
            List<String> employeeCodes, Date weekStart, Integer timesheetYear, Integer timesheetMonth);
    List<TimesheetSummary> findByIdEmployeeCodeInAndIdTimesheetYearAndIdTimesheetMonth(Collection<String> employeeCodes, Integer year, Integer month);

    @Query(value = """
    SELECT dts.project_code, ts.status, COUNT(*)
    FROM daily_time_sheet dts
    JOIN timesheet_summary ts
      ON dts.employee_code = ts.employee_code
     AND dts.timesheet_year = ts.timesheet_year
     AND dts.timesheet_month = ts.timesheet_month
    WHERE dts.project_code IN (:projectCodes)
    GROUP BY dts.project_code, ts.status
    """, nativeQuery = true)
    List<Object[]> countStatusByProjectCode(@Param("projectCodes") List<String> projectCodes);


    @Query("SELECT ts.status, COUNT(ts.id) FROM TimesheetSummary ts " +
            "WHERE ts.approvedBy = :managerCode " +
            "AND (:year IS NULL OR ts.id.timesheetYear = :year) " +
            "AND (:month IS NULL OR ts.id.timesheetMonth = :month) " +
            "GROUP BY ts.status")
    List<Object[]> countTimesheetStatusByManager(String managerCode, Integer year, Integer month);



}
 