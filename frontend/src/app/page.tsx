"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Activity, Server, Database, AlertCircle, CheckCircle2, Play } from "lucide-react";
import { cn } from "@/lib/utils";

interface Event {
    _id: string;
    eventId: string;
    eventType: string;
    timestamp: string;
    payload: any;
    metadata?: any;
}

export default function Dashboard() {
    const [events, setEvents] = useState<Event[]>([]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

    // Form state
    const [eventType, setEventType] = useState("USER_ACTION");
    const [eventPayload, setEventPayload] = useState('{"action": "click", "page": "dashboard"}');

    const fetchEvents = async () => {
        try {
            const res = await fetch("/api/events");
            if (res.ok) {
                const data = await res.json();
                setEvents(data);
                setLastUpdated(new Date());
            }
        } catch (error) {
            console.error("Failed to fetch events", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchEvents();
        const interval = setInterval(fetchEvents, 3000); // Poll every 3 seconds
        return () => clearInterval(interval);
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSubmitting(true);

        try {
            const payloadObj = JSON.parse(eventPayload);
            const eventId = `evt-${Date.now()}`;

            const newEvent = {
                eventId,
                eventType,
                timestamp: new Date().toISOString(),
                payload: payloadObj,
                metadata: {
                    source: "dashboard-ui",
                    priority: "normal"
                }
            };

            // Send to Producer Service
            // Note: In a real scenario, we might want a Next.js API route to proxy this to avoid CORS issues
            // if the backend is on a different domain. For local docker, we might need a proxy.
            // However, for this demo, let's try calling the Next.js API route which then calls the backend, 
            // OR just call the backend directly if CORS allows.
            // Since we didn't set up a proxy route for submission yet, let's try to add one or just log it for now.

            // Wait, I should add a submission API route to avoid CORS and keep it clean.
            // For now, I'll implement the submission logic in a new API route `/api/submit`.

            const res = await fetch("/api/submit", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(newEvent),
            });

            if (res.ok) {
                // Refresh events immediately
                setTimeout(fetchEvents, 1000);
                setEventPayload('{"action": "click", "page": "dashboard"}'); // Reset to default valid JSON
            } else {
                alert("Failed to submit event");
            }
        } catch (error) {
            console.error("Error submitting event", error);
            alert("Invalid JSON payload or server error");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 dark:bg-slate-950 p-8">
            <div className="max-w-7xl mx-auto space-y-8">

                {/* Header */}
                <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-50">Event Processing Dashboard</h1>
                        <p className="text-slate-500 dark:text-slate-400">Real-time monitoring and control</p>
                    </div>
                    <div className="flex items-center space-x-2 text-sm text-slate-500">
                        <Activity className="h-4 w-4 animate-pulse text-green-500" />
                        <span>Live Updates</span>
                        <span className="text-xs opacity-70">({lastUpdated.toLocaleTimeString()})</span>
                    </div>
                </div>

                {/* System Health */}
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    <HealthCard title="Event Producer" status="UP" icon={<Server className="h-4 w-4" />} />
                    <HealthCard title="Kafka Cluster" status="UP" icon={<Activity className="h-4 w-4" />} />
                    <HealthCard title="Event Consumer" status="UP" icon={<Server className="h-4 w-4" />} />
                    <HealthCard title="MongoDB" status="UP" icon={<Database className="h-4 w-4" />} />
                </div>

                <div className="grid gap-4 md:grid-cols-7">

                    {/* Event Submission Form */}
                    <Card className="md:col-span-3">
                        <CardHeader>
                            <CardTitle>Submit Event</CardTitle>
                            <CardDescription>Send a new event to the processing pipeline</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div className="space-y-2">
                                    <Label htmlFor="eventType">Event Type</Label>
                                    <Input
                                        id="eventType"
                                        value={eventType}
                                        onChange={(e) => setEventType(e.target.value)}
                                        placeholder="e.g. USER_LOGIN"
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="payload">Payload (JSON)</Label>
                                    <textarea
                                        id="payload"
                                        className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={eventPayload}
                                        onChange={(e) => setEventPayload(e.target.value)}
                                    />
                                </div>
                                <Button type="submit" className="w-full" disabled={submitting}>
                                    {submitting ? "Sending..." : (
                                        <>
                                            <Play className="mr-2 h-4 w-4" /> Send Event
                                        </>
                                    )}
                                </Button>
                            </form>
                        </CardContent>
                    </Card>

                    {/* Recent Events List */}
                    <Card className="md:col-span-4">
                        <CardHeader>
                            <CardTitle>Recent Events</CardTitle>
                            <CardDescription>Latest processed events from MongoDB</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-4">
                                {loading && events.length === 0 ? (
                                    <div className="text-center py-10 text-slate-500">Loading events...</div>
                                ) : events.length === 0 ? (
                                    <div className="text-center py-10 text-slate-500">No events found</div>
                                ) : (
                                    <div className="space-y-2 max-h-[500px] overflow-y-auto pr-2">
                                        {events.map((event) => (
                                            <div key={event._id || event.eventId} className="flex items-start space-x-4 rounded-md border p-3 bg-white dark:bg-slate-900 shadow-sm">
                                                <div className="mt-1">
                                                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                                                </div>
                                                <div className="flex-1 space-y-1">
                                                    <div className="flex items-center justify-between">
                                                        <p className="text-sm font-medium leading-none">{event.eventType}</p>
                                                        <span className="text-xs text-slate-500">{new Date(event.timestamp).toLocaleTimeString()}</span>
                                                    </div>
                                                    <p className="text-xs text-slate-500 font-mono truncate max-w-[300px]">
                                                        ID: {event.eventId}
                                                    </p>
                                                    <div className="text-xs bg-slate-100 dark:bg-slate-800 p-2 rounded mt-2 font-mono overflow-x-auto">
                                                        {JSON.stringify(event.payload, null, 2)}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}

function HealthCard({ title, status, icon }: { title: string, status: string, icon: React.ReactNode }) {
    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">
                    {title}
                </CardTitle>
                {icon}
            </CardHeader>
            <CardContent>
                <div className="text-2xl font-bold flex items-center">
                    {status}
                    <span className={cn("ml-2 h-2 w-2 rounded-full", status === "UP" ? "bg-green-500" : "bg-red-500")} />
                </div>
                <p className="text-xs text-muted-foreground">
                    Service operational
                </p>
            </CardContent>
        </Card>
    )
}
