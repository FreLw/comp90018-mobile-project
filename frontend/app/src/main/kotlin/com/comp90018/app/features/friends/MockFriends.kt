package com.comp90018.app.features.friends

import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.SearchUser

/** Frontend-only directory used to exercise social UI without writing seed data to Firestore. */
val mockFriendDirectory = listOf(
    SearchUser("mock_zhuoer", "zhuoer", "Zhuoer Chen", "female", "Designing small adventures around Melbourne.", "zhuoer@student.unimelb.edu.au", department = "Faculty of Engineering and IT", major = "Computing and Software Systems"),
    SearchUser("mock_liang", "liang", "Liang Zhou", "male", "Coffee, code, and campus history.", "liang@student.unimelb.edu.au", department = "Faculty of Engineering and IT", major = "Data Science"),
    SearchUser("mock_wang", "wang", "Alex Wang", "prefer_not_to_say", "Always ready for the next clue.", "wang@student.unimelb.edu.au", department = "Faculty of Arts", major = "Digital Humanities"),
    SearchUser("mock_mia", "mia", "Mia Taylor", "female", "Museum lover and weekend photographer.", "mia@student.unimelb.edu.au", department = "Faculty of Arts", major = "Art History"),
    SearchUser("mock_noah", "noah", "Noah Smith", "male", "Finding the quietest corners of campus.", "noah@student.unimelb.edu.au", department = "Faculty of Science", major = "Geography"),
    SearchUser("mock_ava", "ava", "Ava Patel", "female", "Puzzle solver, runner, and curious explorer.", "ava@student.unimelb.edu.au", department = "Faculty of Business and Economics", major = "Economics"),
    SearchUser("mock_oliver", "oliver", "Oliver Brown", "male", "Here for teamwork and difficult riddles.", "oliver@student.unimelb.edu.au", department = "Melbourne Law School", major = "Juris Doctor"),
    SearchUser("mock_sophia", "sophia", "Sophia Lee", "female", "Collecting stories from every building.", "sophia@student.unimelb.edu.au", department = "Faculty of Architecture, Building and Planning", major = "Architecture"),
    SearchUser("mock_ethan", "ethan", "Ethan Wilson", "male", "Map reader and amateur historian.", "ethan@student.unimelb.edu.au", department = "Faculty of Science", major = "Mathematics"),
    SearchUser("mock_emma", "emma", "Emma Davis", "prefer_not_to_say", "Ask me about the Old Quad.", "emma@student.unimelb.edu.au", department = "Faculty of Education", major = "Education"),
)

val mockIncomingRequests = listOf(
    IncomingFriendRequest("mock_mia", "mia"),
    IncomingFriendRequest("mock_liang", "liang"),
)
