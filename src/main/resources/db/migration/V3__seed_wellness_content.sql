insert into content_sources (id, title, publisher, url, locale, review_status, active, created_at, updated_at)
values
    ('11111111-1111-1111-1111-111111111111', 'Menstrual cramps self-care basics', 'AI-Friend Curated Wellness Notes', 'https://example.org/wellness/menstrual-cramps-self-care', 'en-US', 'APPROVED', true, current_timestamp, current_timestamp),
    ('22222222-2222-2222-2222-222222222222', 'PMS nutrition and hydration basics', 'AI-Friend Curated Wellness Notes', 'https://example.org/wellness/pms-nutrition-hydration', 'en-US', 'APPROVED', true, current_timestamp, current_timestamp),
    ('33333333-3333-3333-3333-333333333333', 'Gentle exercise during PMS or period', 'AI-Friend Curated Wellness Notes', 'https://example.org/wellness/period-exercise-basics', 'en-US', 'APPROVED', true, current_timestamp, current_timestamp),
    ('44444444-4444-4444-4444-444444444444', 'When to seek urgent care for period symptoms', 'AI-Friend Curated Wellness Notes', 'https://example.org/wellness/period-red-flags', 'en-US', 'APPROVED', true, current_timestamp, current_timestamp);

insert into content_chunks (id, source_id, topic, chunk_text, keywords, review_status, active, created_at, updated_at)
values
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        '11111111-1111-1111-1111-111111111111',
        'menstrual-cramps',
        'For mild menstrual cramps, general self-care may include heat on the lower abdomen, rest, hydration, gentle movement, and tracking symptoms over time. Severe, worsening, or unusual pain should be discussed with a clinician.',
        'cramps,pain,period pain,menstrual pain,heat,hydration,self care',
        'APPROVED',
        true,
        current_timestamp,
        current_timestamp
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        '22222222-2222-2222-2222-222222222222',
        'pms-nutrition',
        'For PMS, many people find it helpful to focus on regular meals, enough water, fiber-rich foods, protein, and limiting excess caffeine or alcohol if those worsen symptoms. Food advice should stay general and non-prescriptive.',
        'pms,food,diet,nutrition,hydration,water,caffeine,alcohol,protein,fiber',
        'APPROVED',
        true,
        current_timestamp,
        current_timestamp
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
        '33333333-3333-3333-3333-333333333333',
        'period-exercise',
        'During PMS or a period, gentle movement such as walking, stretching, yoga, or light exercise may help some people feel better. Users should listen to their body and avoid pushing through severe pain, dizziness, or heavy bleeding.',
        'exercise,workout,walk,walking,stretching,yoga,pms,period movement',
        'APPROVED',
        true,
        current_timestamp,
        current_timestamp
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
        '44444444-4444-4444-4444-444444444444',
        'red-flags',
        'Urgent medical attention may be needed for severe pelvic pain, very heavy bleeding, fainting, chest pain, pregnancy-related bleeding, or thoughts of self-harm. AI-Friend should route these situations to urgent care rather than providing routine wellness guidance.',
        'urgent,severe pain,heavy bleeding,fainting,chest pain,pregnant bleeding,self harm,emergency',
        'APPROVED',
        true,
        current_timestamp,
        current_timestamp
    );
