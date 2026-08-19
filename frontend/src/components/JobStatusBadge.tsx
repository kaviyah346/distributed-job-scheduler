import React from 'react';
import { JobStatus } from '@/lib/types';
import { Clock, Play, CheckCircle2, XCircle, AlertTriangle, Calendar, ShieldAlert } from 'lucide-react';

interface Props {
  status: JobStatus | string;
  size?: 'sm' | 'md' | 'lg';
}

export const JobStatusBadge: React.FC<Props> = ({ status, size = 'md' }) => {
  const sizeClasses = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-xs font-medium',
    lg: 'px-3 py-1.5 text-sm font-semibold',
  }[size];

  switch (status) {
    case 'QUEUED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-blue-950/80 text-blue-400 border border-blue-800/60 ${sizeClasses}`}>
          <Clock className="w-3.5 h-3.5" />
          QUEUED
        </span>
      );
    case 'SCHEDULED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-indigo-950/80 text-indigo-400 border border-indigo-800/60 ${sizeClasses}`}>
          <Calendar className="w-3.5 h-3.5" />
          SCHEDULED
        </span>
      );
    case 'CLAIMED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-amber-950/80 text-amber-300 border border-amber-800/60 ${sizeClasses}`}>
          <Clock className="w-3.5 h-3.5 animate-spin" />
          CLAIMED
        </span>
      );
    case 'RUNNING':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-cyan-950/80 text-cyan-300 border border-cyan-700/60 animate-pulse ${sizeClasses}`}>
          <Play className="w-3.5 h-3.5 fill-cyan-300" />
          RUNNING
        </span>
      );
    case 'COMPLETED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-emerald-950/80 text-emerald-400 border border-emerald-800/60 ${sizeClasses}`}>
          <CheckCircle2 className="w-3.5 h-3.5" />
          COMPLETED
        </span>
      );
    case 'FAILED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-rose-950/80 text-rose-400 border border-rose-800/60 ${sizeClasses}`}>
          <XCircle className="w-3.5 h-3.5" />
          FAILED
        </span>
      );
    case 'DEAD_LETTERED':
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-red-950 text-red-400 border border-red-800 font-bold ${sizeClasses}`}>
          <ShieldAlert className="w-3.5 h-3.5" />
          DEAD_LETTERED
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center gap-1.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700 ${sizeClasses}`}>
          <AlertTriangle className="w-3.5 h-3.5" />
          {status}
        </span>
      );
  }
};
