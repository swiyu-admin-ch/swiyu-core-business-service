-- Add liability_shift column to payment table for 3D-Secure liability shift indicator
ALTER TABLE payment
    ADD COLUMN liability_shift BOOLEAN;