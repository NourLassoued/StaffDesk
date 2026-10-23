package com.northpeak.hrms.model;

/**
 * Annual leave balance for one employee, in working days.
 *
 * @param allowance yearly annual-leave allowance
 * @param used      annual days already approved / taken
 * @param pending   annual days in requests awaiting a decision
 * @param remaining allowance minus used
 * @param sickUsed  approved sick-leave days
 */
public record LeaveBalance(int allowance, int used, int pending, int remaining, int sickUsed) {

    public int remainingAfterPending() {
        return remaining - pending;
    }
}
