import { Client, type StompSubscription } from '@stomp/stompjs';
import { useEffect, useRef } from 'react';
import { getToken } from './auth';

/**
 * One shared STOMP-over-WebSocket connection to notification-service.
 * Subscriptions made before the socket is connected are queued and
 * re-created automatically after every (re)connect.
 */
interface Entry {
  topic: string;
  callback: (body: unknown) => void;
  subscription?: StompSubscription;
}

const entries = new Set<Entry>();
let client: Client | null = null;

function socketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/ws`;
}

function attach(entry: Entry) {
  if (!client?.connected) return;
  entry.subscription = client.subscribe(entry.topic, (message) => entry.callback(JSON.parse(message.body)));
}

export function connectWs() {
  if (client) return;
  client = new Client({
    brokerURL: socketUrl(),
    reconnectDelay: 3000,
    // the JWT is checked by notification-service when the STOMP session opens
    beforeConnect: () => {
      client!.connectHeaders = { Authorization: `Bearer ${getToken() ?? ''}` };
    },
    onConnect: () => entries.forEach(attach),
  });
  client.activate();
}

export function disconnectWs() {
  entries.forEach((e) => (e.subscription = undefined));
  client?.deactivate();
  client = null;
}

export function subscribe(topic: string, callback: (body: unknown) => void): () => void {
  connectWs();
  const entry: Entry = { topic, callback };
  entries.add(entry);
  attach(entry);
  return () => {
    entries.delete(entry);
    if (client?.connected) entry.subscription?.unsubscribe();
  };
}

/** React hook: subscribe to a topic while the component is mounted (pass null to skip). */
export function useTopic<T>(topic: string | null, onMessage: (body: T) => void) {
  const handler = useRef(onMessage);
  handler.current = onMessage;

  useEffect(() => {
    if (!topic) return;
    return subscribe(topic, (body) => handler.current(body as T));
  }, [topic]);
}
