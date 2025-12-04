import { NextResponse } from 'next/server';
import clientPromise from '@/lib/mongodb';

export async function GET() {
    try {
        const client = await clientPromise;
        const db = client.db("event_processing");

        // Fetch recent events, sorted by timestamp descending
        // Limit to 50 for performance
        const events = await db
            .collection("events")
            .find({})
            .sort({ timestamp: -1 })
            .limit(50)
            .toArray();

        return NextResponse.json(events);
    } catch (e) {
        console.error(e);
        return NextResponse.json({ error: 'Failed to fetch events' }, { status: 500 });
    }
}
