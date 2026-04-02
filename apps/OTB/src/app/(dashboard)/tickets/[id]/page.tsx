'use client';
import { useState, useEffect, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { TicketDetailPage } from '@/features/tickets';

export default function TicketDetailRoute() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const showApprovalActions = searchParams.get('source') === 'approvals';

  // Initialize state via lazy initializer to avoid cascading renders
  const [ticket, setTicket] = useState<any>(() => {
    if (typeof window !== 'undefined') {
      const stored = sessionStorage.getItem('selectedTicket');
      return stored ? JSON.parse(stored) : null;
    }
    return null;
  });

  const handleBack = useCallback(() => {
    sessionStorage.removeItem('selectedTicket');
    router.push(showApprovalActions ? '/approvals' : '/tickets');
  }, [router, showApprovalActions]);

  return (
    <TicketDetailPage
      ticket={ticket}
      onBack={handleBack}
      showApprovalActions={showApprovalActions}
    />
  );
}
