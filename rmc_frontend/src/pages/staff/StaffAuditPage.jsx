import { useEffect, useState } from 'react'
import { StaffAlert, StaffPageShell } from '@/components/staff/StaffPageShell'
import {
  StaffTable,
  StaffTableBody,
  StaffTableCell,
  StaffTableHead,
  StaffTableHeader,
  StaffTableRow,
  StaffTableWrap,
} from '@/components/staff/StaffTable'
import StaffTablePagination, { STAFF_PAGE_SIZE_OPTIONS } from '@/components/staff/StaffTablePagination'
import {
  StaffPageTabContent,
  StaffPageTabList,
  StaffPageTabs,
  StaffPageTabTrigger,
} from '@/components/staff/StaffPageTabs'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  activityActionBadgeClass,
  auditStatusBadgeClass,
  formatActivityAction,
  formatAuditStatus,
  formatAuditTimestamp,
  formatLoginEvent,
  formatStaffRole,
  loginEventBadgeClass,
} from '@/lib/formatAudit'
import { getStaffActivityAudit, getStaffLoginAudit } from '@/staffApi'

const EMPTY_PAGE = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
}

function AuditTablePager({ page, size, totalElements, totalPages, onPageChange, onPageSizeChange }) {
  return (
    <StaffTablePagination
      page={page}
      pageSize={size}
      totalElements={totalElements}
      totalPages={totalPages}
      onPageChange={onPageChange}
      onPageSizeChange={onPageSizeChange}
      pageSizeOptions={STAFF_PAGE_SIZE_OPTIONS}
    />
  )
}

export default function StaffAuditPage() {
  const [loginPage, setLoginPage] = useState(EMPTY_PAGE)
  const [activityPage, setActivityPage] = useState(EMPTY_PAGE)
  const [loginPageIndex, setLoginPageIndex] = useState(0)
  const [activityPageIndex, setActivityPageIndex] = useState(0)
  const [loginPageSize, setLoginPageSize] = useState(10)
  const [activityPageSize, setActivityPageSize] = useState(10)
  const [loginLoading, setLoginLoading] = useState(true)
  const [activityLoading, setActivityLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadLoginAudit()
  }, [loginPageIndex, loginPageSize])

  useEffect(() => {
    loadActivityAudit()
  }, [activityPageIndex, activityPageSize])

  async function loadLoginAudit() {
    setLoginLoading(true)
    setError('')
    try {
      const data = await getStaffLoginAudit({ page: loginPageIndex, size: loginPageSize })
      setLoginPage(data || EMPTY_PAGE)
      if (data?.totalPages > 0 && loginPageIndex >= data.totalPages) {
        setLoginPageIndex(Math.max(data.totalPages - 1, 0))
      }
    } catch (err) {
      setError(err.message)
      setLoginPage(EMPTY_PAGE)
    } finally {
      setLoginLoading(false)
    }
  }

  async function loadActivityAudit() {
    setActivityLoading(true)
    setError('')
    try {
      const data = await getStaffActivityAudit({ page: activityPageIndex, size: activityPageSize })
      setActivityPage(data || EMPTY_PAGE)
      if (data?.totalPages > 0 && activityPageIndex >= data.totalPages) {
        setActivityPageIndex(Math.max(data.totalPages - 1, 0))
      }
    } catch (err) {
      setError(err.message)
      setActivityPage(EMPTY_PAGE)
    } finally {
      setActivityLoading(false)
    }
  }

  function handleLoginPageSizeChange(nextSize) {
    setLoginPageSize(nextSize)
    setLoginPageIndex(0)
  }

  function handleActivityPageSizeChange(nextSize) {
    setActivityPageSize(nextSize)
    setActivityPageIndex(0)
  }

  const loginRows = loginPage.content || []
  const activityRows = activityPage.content || []

  return (
    <StaffPageShell
      title="Audit logs"
      description="Review staff sign-in activity and module actions across the portal."
    >
      <StaffAlert>{error}</StaffAlert>

      <StaffPageTabs defaultValue="login">
        <StaffPageTabList>
          <StaffPageTabTrigger value="login">Login audit</StaffPageTabTrigger>
          <StaffPageTabTrigger value="activity">User activity history</StaffPageTabTrigger>
        </StaffPageTabList>

        <StaffPageTabContent value="login">
          <Card className="overflow-hidden">
            <CardHeader>
              <CardTitle>Login audit</CardTitle>
              <CardDescription>
                Successful and failed staff login and logout events with IP address and failure
                reason.
              </CardDescription>
            </CardHeader>
            <CardContent className="p-0">
              {loginLoading ? (
                <p className="px-6 pb-6 text-sm text-muted-foreground">Loading login audit…</p>
              ) : loginRows.length === 0 ? (
                <p className="px-6 pb-6 text-sm text-muted-foreground">No login events recorded yet.</p>
              ) : (
                <>
                  <StaffTableWrap>
                    <StaffTable>
                      <StaffTableHeader>
                        <StaffTableRow>
                          <StaffTableHead>Timestamp</StaffTableHead>
                          <StaffTableHead>Full name</StaffTableHead>
                          <StaffTableHead>Event</StaffTableHead>
                          <StaffTableHead>Status</StaffTableHead>
                          <StaffTableHead>IP address</StaffTableHead>
                          <StaffTableHead>Failure reason</StaffTableHead>
                        </StaffTableRow>
                      </StaffTableHeader>
                      <StaffTableBody>
                        {loginRows.map((entry) => (
                          <StaffTableRow key={entry.id}>
                            <StaffTableCell className="whitespace-nowrap">
                              {formatAuditTimestamp(entry.createdAt)}
                            </StaffTableCell>
                            <StaffTableCell>{entry.fullName || entry.email || '—'}</StaffTableCell>
                            <StaffTableCell>
                              <Badge variant="outline" className={loginEventBadgeClass(entry.event)}>
                                {formatLoginEvent(entry.event)}
                              </Badge>
                            </StaffTableCell>
                            <StaffTableCell>
                              <Badge variant="outline" className={auditStatusBadgeClass(entry.status)}>
                                {formatAuditStatus(entry.status)}
                              </Badge>
                            </StaffTableCell>
                            <StaffTableCell>{entry.ipAddress || '—'}</StaffTableCell>
                            <StaffTableCell>{entry.failureReason || '—'}</StaffTableCell>
                          </StaffTableRow>
                        ))}
                      </StaffTableBody>
                    </StaffTable>
                  </StaffTableWrap>
                  <AuditTablePager
                    page={loginPageIndex}
                    size={loginPageSize}
                    totalElements={loginPage.totalElements || 0}
                    totalPages={loginPage.totalPages || 0}
                    onPageChange={setLoginPageIndex}
                    onPageSizeChange={handleLoginPageSizeChange}
                  />
                </>
              )}
            </CardContent>
          </Card>
        </StaffPageTabContent>

        <StaffPageTabContent value="activity">
          <Card className="overflow-hidden">
            <CardHeader>
              <CardTitle>User activity history</CardTitle>
              <CardDescription>
                Recent staff actions grouped by module, including view, edit, add, and delete
                operations.
              </CardDescription>
            </CardHeader>
            <CardContent className="p-0">
              {activityLoading ? (
                <p className="px-6 pb-6 text-sm text-muted-foreground">Loading activity history…</p>
              ) : activityRows.length === 0 ? (
                <p className="px-6 pb-6 text-sm text-muted-foreground">No user activity recorded yet.</p>
              ) : (
                <>
                  <StaffTableWrap>
                    <StaffTable>
                      <StaffTableHeader>
                        <StaffTableRow>
                          <StaffTableHead>Timestamp</StaffTableHead>
                          <StaffTableHead>Full name</StaffTableHead>
                          <StaffTableHead>Role</StaffTableHead>
                          <StaffTableHead>Action</StaffTableHead>
                          <StaffTableHead>Module</StaffTableHead>
                        </StaffTableRow>
                      </StaffTableHeader>
                      <StaffTableBody>
                        {activityRows.map((entry) => (
                          <StaffTableRow key={entry.id}>
                            <StaffTableCell className="whitespace-nowrap">
                              {formatAuditTimestamp(entry.createdAt)}
                            </StaffTableCell>
                            <StaffTableCell>{entry.fullName}</StaffTableCell>
                            <StaffTableCell>{formatStaffRole(entry.role)}</StaffTableCell>
                            <StaffTableCell>
                              <Badge
                                variant="outline"
                                className={activityActionBadgeClass(entry.action)}
                              >
                                {formatActivityAction(entry.action)}
                              </Badge>
                            </StaffTableCell>
                            <StaffTableCell>{entry.moduleLabel}</StaffTableCell>
                          </StaffTableRow>
                        ))}
                      </StaffTableBody>
                    </StaffTable>
                  </StaffTableWrap>
                  <AuditTablePager
                    page={activityPageIndex}
                    size={activityPageSize}
                    totalElements={activityPage.totalElements || 0}
                    totalPages={activityPage.totalPages || 0}
                    onPageChange={setActivityPageIndex}
                    onPageSizeChange={handleActivityPageSizeChange}
                  />
                </>
              )}
            </CardContent>
          </Card>
        </StaffPageTabContent>
      </StaffPageTabs>
    </StaffPageShell>
  )
}
