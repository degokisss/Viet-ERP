'use client';
import { useState, useEffect, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useAppContext } from '@/contexts/AppContext';
import { proposalService } from '@/services';
import { ProposalDetailPage } from '@/features/otb';

export default function ProposalDetailRoute() {
  const router = useRouter();
  const params = useParams();
  const { registerSave, unregisterSave } = useAppContext();
  const saveRef = { current: null as any };

  // Initialize state via lazy initializer to avoid cascading renders
  const [proposal, setProposal] = useState<any>(() => {
    if (typeof window !== 'undefined') {
      const stored = sessionStorage.getItem('selectedProposal');
      return stored ? JSON.parse(stored) : null;
    }
    return null;
  });

  useEffect(() => {
    if (proposal === null && params.id) {
      proposalService.getOne(params.id as string)
        .then((data: any) => setProposal(data))
        .catch(() => {});
    }
  }, [params.id, proposal]);

  const handleBack = useCallback(() => {
    sessionStorage.removeItem('selectedProposal');
    router.back();
  }, [router]);

  const handleSave = useCallback((_data?: any) => {
    handleBack();
  }, [handleBack]);

  // Register save handler for AppHeader Save button
  useEffect(() => {
    registerSave(handleSave);
    return () => unregisterSave();
  }, [registerSave, unregisterSave, handleSave]);

  return (
    <ProposalDetailPage
      proposal={proposal}
      onBack={handleBack}
      onSave={handleSave}
      entityId={params.id as string}
    />
  );
}
