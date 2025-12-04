import { NextResponse } from 'next/server';

export async function POST(request: Request) {
    try {
        const body = await request.json();

        // Forward to Event Producer Service
        // Assuming the service is running on localhost:8081
        const backendUrl = process.env.EVENT_PRODUCER_URL || 'http://localhost:8081/api/events';

        const res = await fetch(backendUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            return NextResponse.json(
                { error: 'Backend submission failed', details: errorData },
                { status: res.status }
            );
        }

        const data = await res.json();
        return NextResponse.json(data);

    } catch (error) {
        console.error('Submission proxy error:', error);
        return NextResponse.json(
            { error: 'Internal server error during submission' },
            { status: 500 }
        );
    }
}
