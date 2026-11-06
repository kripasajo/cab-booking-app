import type { RideStatus } from '../types';

const STEPS = ['Finding your driver', 'Driver on the way', 'On the trip', 'Arrived'];

const STEP_INDEX: Partial<Record<RideStatus, number>> = {
  REQUESTED: 0,
  DRIVER_ASSIGNED: 1,
  IN_PROGRESS: 2,
  COMPLETED: 4, // every stop done
};

/** The ride lifecycle drawn as a route line: done stops are filled, the current one pulses. */
export default function RideProgress({ status, detail }: { status: RideStatus; detail?: string }) {
  const current = STEP_INDEX[status] ?? 0;
  return (
    <ol className="progress" aria-label="Ride progress">
      {STEPS.map((label, i) => {
        const state = i < current ? 'done' : i === current ? 'now' : 'todo';
        return (
          <li key={label} className={`progress-step ${state}`} aria-current={state === 'now' ? 'step' : undefined}>
            <span className="progress-dot" aria-hidden="true" />
            <span className="progress-label">{label}</span>
            {state === 'now' && detail && <span className="progress-detail">{detail}</span>}
          </li>
        );
      })}
    </ol>
  );
}
